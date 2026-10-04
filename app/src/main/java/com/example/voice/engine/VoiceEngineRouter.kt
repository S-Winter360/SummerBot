package com.example.voice.engine

import com.example.core.event.EventPriority
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import com.example.voice.VoiceEngine
import com.example.voice.models.SpeechState
import com.example.voice.models.VoiceDiagnostics
import com.example.voice.models.VoiceProfile
import com.example.voice.models.VoiceProfileId
import com.example.voice.models.VoiceProviderType
import com.example.voice.provider.DefaultNaturalVoiceProvider
import com.example.voice.provider.NaturalVoiceProvider
import com.example.voice.provider.VoiceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production-ready voice router and engine coordinator for Summer.
 * Decouples speech generation from specific TTS technology, selecting the best available
 * offline provider (Natural Local when available, otherwise System Offline TTS fallback).
 *
 * Implements conversation interruptibility, safe sentence chunking, bounded queuing,
 * and transparent diagnostics.
 */
class VoiceEngineRouter(
    val naturalVoiceProvider: NaturalVoiceProvider = DefaultNaturalVoiceProvider(),
    val systemTtsProvider: VoiceProvider? = null,
    val resolver: VoiceProfileResolver = VoiceProfileResolver(),
    val segmenter: SpeechSegmenter = SpeechSegmenter(),
    val queue: SpeechQueue = SpeechQueue(),
    val eventBus: SummerEventBus? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : VoiceEngine {

    companion object {
        private const val TAG = "VoiceEngineRouter"
    }

    private val _currentProfile = MutableStateFlow<VoiceProfile?>(VoiceProfile.SUMMER_FEMALE)
    override val currentProfile: StateFlow<VoiceProfile?> = _currentProfile.asStateFlow()

    private val _availableProfiles = MutableStateFlow(VoiceProfile.BUILT_IN_PROFILES)
    override val availableProfiles: StateFlow<List<VoiceProfile>> = _availableProfiles.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechState = MutableStateFlow(SpeechState.IDLE)
    override val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        VoiceDiagnostics(
            systemActive = true,
            activeProvider = determineActiveProviderType(),
            selectedProfile = VoiceProfileId.FEMALE,
            isOfflineCapable = true,
            isNaturalNeuralAvailable = naturalVoiceProvider.isAvailable,
            statusMessage = "Initialized"
        )
    )
    override val diagnostics: StateFlow<VoiceDiagnostics> = _diagnostics.asStateFlow()

    override val isAvailable: Boolean
        get() = naturalVoiceProvider.isAvailable || (systemTtsProvider?.isAvailable == true)

    private var activePlaybackJob: Job? = null

    init {
        scope.launch {
            initializeProviders()
        }
    }

    suspend fun initializeProviders() = withContext(Dispatchers.IO) {
        try {
            if (naturalVoiceProvider.isAvailable) {
                naturalVoiceProvider.initialize()
            }
            systemTtsProvider?.initialize()
            refreshDiagnostics()
        } catch (t: Throwable) {
            logWarn("Provider initialization warning: ${t.message}")
        }
    }

    fun selectVoiceProfile(profileId: VoiceProfileId) {
        val targetProfile = VoiceProfile.fromId(profileId)
        _currentProfile.value = targetProfile
        refreshDiagnostics()
    }

    fun updateProfileSettings(speechRate: Float, pitch: Float, volume: Float = 1.0f) {
        _currentProfile.update { current ->
            (current ?: VoiceProfile.SUMMER_FEMALE).copy(
                speechRate = speechRate.coerceIn(0.5f, 2.0f),
                pitch = pitch.coerceIn(0.5f, 2.0f),
                volume = volume.coerceIn(0.0f, 1.0f)
            )
        }
        refreshDiagnostics()
    }

    override suspend fun speak(text: String, profile: VoiceProfile?): Boolean {
        val targetProfile = profile ?: _currentProfile.value ?: VoiceProfile.SUMMER_FEMALE
        _currentProfile.value = targetProfile

        if (text.isBlank()) return false

        // Interrupt any ongoing speech immediately (conversational interruptibility policy)
        stop()

        val provider = getActiveProvider()
        if (provider == null) {
            logWarn("No speech provider available to handle speak request.")
            _speechState.value = SpeechState.ERROR
            publishEvent(
                SummerEvent.SpeechError(
                    errorMessage = "No speech provider is available",
                    textSnippet = text.take(50)
                )
            )
            return false
        }

        // Segment response text into natural, bounded chunks
        val chunks = segmenter.segment(text)
        if (chunks.isEmpty()) return false

        // Enqueue speech chunks into bounded queue
        for (chunk in chunks) {
            queue.enqueue(SpeechItem(text = chunk, profile = targetProfile))
        }

        // Launch sequential playback job
        val playbackJob = scope.launch {
            executePlaybackLoop(provider, targetProfile)
        }
        activePlaybackJob = playbackJob

        return true
    }

    private suspend fun executePlaybackLoop(provider: VoiceProvider, profile: VoiceProfile) {
        _isSpeaking.value = true
        _speechState.value = SpeechState.SPEAKING
        publishEvent(
            SummerEvent.SpeechStarted(
                profileId = profile.id.name,
                providerType = provider.providerType.name,
                totalChunks = queue.size
            )
        )

        try {
            var chunkIndex = 0
            while (queue.size > 0) {
                val item = queue.poll() ?: break
                chunkIndex++

                publishEvent(
                    SummerEvent.SpeechProgress(
                        currentChunk = chunkIndex,
                        textSnippet = item.text.take(30)
                    )
                )

                val deferredChunk = kotlinx.coroutines.CompletableDeferred<Boolean>()

                provider.speak(
                    text = item.text,
                    profile = item.profile,
                    onStarted = {},
                    onDone = { deferredChunk.complete(true) },
                    onError = { err ->
                        logWarn("Chunk speech error: $err")
                        deferredChunk.complete(false)
                    }
                )

                deferredChunk.await()
            }

            _speechState.value = SpeechState.IDLE
            _isSpeaking.value = false
            publishEvent(SummerEvent.SpeechCompleted(profileId = profile.id.name))

        } catch (e: CancellationException) {
            logInfo("Playback cancelled / interrupted.")
            _speechState.value = SpeechState.STOPPED
            _isSpeaking.value = false
            publishEvent(SummerEvent.SpeechStopped(reason = "Interrupted by user action"))
        } catch (t: Throwable) {
            logWarn("Playback exception: ${t.message}")
            _speechState.value = SpeechState.ERROR
            _isSpeaking.value = false
            publishEvent(SummerEvent.SpeechError(errorMessage = t.message ?: "Playback fault"))
        } finally {
            _isSpeaking.value = false
            refreshDiagnostics()
        }
    }

    override suspend fun stop() {
        queue.clear()
        activePlaybackJob?.cancel()
        activePlaybackJob = null

        getActiveProvider()?.stop()

        _isSpeaking.value = false
        _speechState.value = SpeechState.STOPPED
        refreshDiagnostics()
    }

    fun getActiveProvider(): VoiceProvider? {
        return when {
            naturalVoiceProvider.isAvailable -> naturalVoiceProvider
            systemTtsProvider?.isAvailable == true -> systemTtsProvider
            else -> null
        }
    }

    fun determineActiveProviderType(): VoiceProviderType {
        return when {
            naturalVoiceProvider.isAvailable -> VoiceProviderType.NATURAL_LOCAL
            systemTtsProvider?.isAvailable == true -> VoiceProviderType.SYSTEM_OFFLINE
            else -> VoiceProviderType.SYSTEM_OFFLINE
        }
    }

    fun refreshDiagnostics() {
        val provider = getActiveProvider()
        val current = _currentProfile.value ?: VoiceProfile.SUMMER_FEMALE
        val engineVoices = provider?.getAvailableEngineVoices() ?: emptyList()
        val resolved = resolver.resolve(current, engineVoices)

        _diagnostics.update {
            VoiceDiagnostics(
                systemActive = isAvailable,
                activeProvider = provider?.providerType ?: VoiceProviderType.SYSTEM_OFFLINE,
                selectedProfile = current.id,
                resolvedEngineVoice = resolved.engineVoiceId,
                isOfflineCapable = true,
                isNaturalNeuralAvailable = naturalVoiceProvider.isAvailable,
                availableProfiles = listOf(VoiceProfileId.FEMALE, VoiceProfileId.MALE),
                speechState = _speechState.value,
                speechRate = current.speechRate,
                pitch = current.pitch,
                statusMessage = when {
                    naturalVoiceProvider.isAvailable -> "Active (Natural Local Neural Voice)"
                    systemTtsProvider?.isAvailable == true -> "Active (System Offline Fallback)"
                    else -> "Standby / Initializing Fallback"
                }
            )
        }
    }

    private fun publishEvent(event: SummerEvent) {
        scope.launch {
            eventBus?.publish(event)
        }
    }

    fun release() {
        activePlaybackJob?.cancel()
        queue.clear()
        systemTtsProvider?.release()
        naturalVoiceProvider.release()
    }

    private fun logInfo(msg: String) {
        try {
            android.util.Log.i(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun logWarn(msg: String) {
        try {
            android.util.Log.w(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }
}
