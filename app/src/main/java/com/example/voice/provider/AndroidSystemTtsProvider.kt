package com.example.voice.provider

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.voice.models.VoiceGender
import com.example.voice.models.VoiceProfile
import com.example.voice.models.VoiceProfileId
import com.example.voice.models.VoiceProviderType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

/**
 * Android TextToSpeech implementation serving as Summer's reliable offline-first fallback provider.
 * Initializes asynchronously without blocking UI, queries available voices, respects locale/rate/pitch,
 * and releases platform resources safely.
 */
class AndroidSystemTtsProvider(
    private val context: Context
) : VoiceProvider {

    companion object {
        private const val TAG = "AndroidSystemTtsProvider"
    }

    override val providerType: VoiceProviderType = VoiceProviderType.SYSTEM_OFFLINE

    private var tts: TextToSpeech? = null

    @Volatile
    override var isInitialized: Boolean = false
        private set

    @Volatile
    override var isAvailable: Boolean = false
        private set

    private var initDeferred: CompletableDeferred<Boolean>? = null
    private val activeCallbacks = mutableMapOf<String, Triple<() -> Unit, () -> Unit, (String) -> Unit>>()

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext isAvailable

        synchronized(this@AndroidSystemTtsProvider) {
            if (initDeferred != null) return@synchronized
            initDeferred = CompletableDeferred()
        }

        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                val success = (status == TextToSpeech.SUCCESS)
                isInitialized = true
                isAvailable = success
                if (success) {
                    configureDefaultTts(tts)
                }
                initDeferred?.complete(success)
            }
        } catch (t: Throwable) {
            logError("Failed to instantiate Android TextToSpeech", t)
            isInitialized = true
            isAvailable = false
            initDeferred?.complete(false)
        }

        initDeferred?.await() ?: false
    }

    private fun configureDefaultTts(engine: TextToSpeech?) {
        try {
            engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    utteranceId?.let { id ->
                        synchronized(activeCallbacks) {
                            activeCallbacks[id]?.first?.invoke()
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { id ->
                        synchronized(activeCallbacks) {
                            activeCallbacks.remove(id)?.second?.invoke()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    utteranceId?.let { id ->
                        synchronized(activeCallbacks) {
                            activeCallbacks.remove(id)?.third?.invoke("TTS playback error")
                        }
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    utteranceId?.let { id ->
                        synchronized(activeCallbacks) {
                            activeCallbacks.remove(id)?.third?.invoke("TTS error code $errorCode")
                        }
                    }
                }
            })
        } catch (t: Throwable) {
            logWarn("Could not set utterance progress listener: ${t.message}")
        }
    }

    override suspend fun speak(
        text: String,
        profile: VoiceProfile,
        onStarted: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        if (!isInitialized) {
            val initialized = initialize()
            if (!initialized) {
                onError("System TTS could not be initialized")
                return@withContext false
            }
        }

        val engine = tts
        if (engine == null || !isAvailable) {
            onError("System TTS is unavailable")
            return@withContext false
        }

        try {
            // Apply voice parameters with safety clamping
            val rate = profile.speechRate.coerceIn(0.5f, 2.0f)
            val pitch = profile.pitch.coerceIn(0.5f, 2.0f)
            val volume = profile.volume.coerceIn(0.0f, 1.0f)

            engine.setSpeechRate(rate)
            engine.setPitch(pitch)

            // Select matching system voice if available
            resolveAndApplyVoice(engine, profile)

            val utteranceId = UUID.randomUUID().toString()
            synchronized(activeCallbacks) {
                activeCallbacks[utteranceId] = Triple(onStarted, onDone, onError)
            }

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
            }

            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                synchronized(activeCallbacks) {
                    activeCallbacks.remove(utteranceId)
                }
                onError("TTS speak call returned status $result")
                return@withContext false
            }
            true
        } catch (t: Throwable) {
            logError("Exception in TTS speak", t)
            onError(t.message ?: "Unknown TTS exception")
            false
        }
    }

    private fun resolveAndApplyVoice(engine: TextToSpeech, profile: VoiceProfile) {
        try {
            val availableVoices = engine.voices ?: emptySet()
            if (availableVoices.isEmpty()) {
                engine.language = profile.locale
                return
            }

            // Target candidate matching profile
            val targetVoice = findBestMatchingVoice(availableVoices, profile)
            if (targetVoice != null) {
                engine.voice = targetVoice
            } else {
                engine.language = profile.locale
            }
        } catch (_: Throwable) {
            try {
                engine.language = profile.locale
            } catch (_: Throwable) {}
        }
    }

    fun findBestMatchingVoice(availableVoices: Set<Voice>, profile: VoiceProfile): Voice? {
        val targetLocale = profile.locale
        val isMale = (profile.gender == VoiceGender.MALE || profile.id == VoiceProfileId.MALE)

        // 1. Exact locale matching voices
        val localeMatches = availableVoices.filter {
            it.locale.language.equals(targetLocale.language, ignoreCase = true)
        }

        val candidates = if (localeMatches.isNotEmpty()) localeMatches else availableVoices.toList()

        // 2. Gender heuristics from voice name
        val matchedVoice = candidates.firstOrNull { voice ->
            val name = voice.name.lowercase(Locale.ROOT)
            if (isMale) {
                (name.contains("male") && !name.contains("female")) ||
                    name.contains("-g-") || name.contains("-d-") || name.contains("en-us-x-sfg") || name.contains("man")
            } else {
                name.contains("female") || name.contains("-f-") || name.contains("-c-") ||
                    name.contains("en-us-x-tpd") || name.contains("woman")
            }
        }

        // 3. Fallback: select any offline/normal latency voice from candidates, or first
        return matchedVoice ?: candidates.firstOrNull { !it.isNetworkConnectionRequired } ?: candidates.firstOrNull()
    }

    override suspend fun stop() = withContext(Dispatchers.Main) {
        try {
            tts?.stop()
            synchronized(activeCallbacks) {
                activeCallbacks.clear()
            }
        } catch (t: Throwable) {
            logWarn("Exception stopping TTS: ${t.message}")
        }
    }

    override fun getAvailableEngineVoices(): List<String> {
        return try {
            tts?.voices?.map { it.name } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            isAvailable = false
            synchronized(activeCallbacks) {
                activeCallbacks.clear()
            }
        } catch (t: Throwable) {
            logWarn("Exception releasing TTS: ${t.message}")
        }
    }

    private fun logWarn(msg: String) {
        try {
            android.util.Log.w(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun logError(msg: String, tr: Throwable?) {
        try {
            android.util.Log.e(TAG, msg, tr)
        } catch (_: Throwable) {
            System.err.println("[$TAG] $msg: ${tr?.message}")
        }
    }
}
