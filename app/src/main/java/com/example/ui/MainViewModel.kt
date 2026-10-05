package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.ActionAuditEntry
import com.example.actions.SecuredActionExecutor
import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.capability.AIDiagnostics
import com.example.ai.capability.AIModelMetadata
import com.example.ai.capability.AIModelRegistry
import com.example.ai.capability.AIModelRouter
import com.example.ai.capability.AIProviderType
import com.example.ai.capability.AndroidDeviceAICapabilityDetector
import com.example.ai.capability.AndroidOnDeviceGenAIStatusProvider
import com.example.ai.capability.DeviceAICapabilityDetector
import com.example.ai.capability.OnDeviceGenAIProvider
import com.example.ai.capability.OnDeviceGenAIStatusProvider
import com.example.ai.localmodel.DefaultEmbeddedModelManager
import com.example.ai.localmodel.EmbeddedLocalAIEngine
import com.example.ai.localmodel.EmbeddedModelDiagnostics
import com.example.ai.localmodel.EmbeddedModelManager
import com.example.ai.localmodel.EmbeddedModelStatus
import com.example.core.decision.DefaultSummerDecisionEngine
import com.example.core.event.SummerEventBus
import com.example.core.interaction.SummerInteraction
import com.example.core.orchestrator.SummerOrchestrator
import com.example.core.personality.SummerPersonality
import com.example.core.response.SummerResponse
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.memory.LocalMemoryRepository
import com.example.memory.MemoryRepository
import com.example.memory.database.SummerDatabase
import com.example.memory.models.SummerSettings
import com.example.network.AndroidNetworkInformationProvider
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.vision.AndroidVisionCapabilityDetector
import com.example.vision.VisionEngine
import com.example.vision.VisionEngineRouter
import com.example.voice.engine.VoiceEngineRouter
import com.example.voice.input.AndroidSpeechRecognitionEngine
import com.example.voice.input.SpeechRecognitionDiagnostics
import com.example.voice.input.SpeechRecognitionEngine
import com.example.voice.input.SpeechRecognitionRouter
import com.example.voice.input.SpeechRecognitionState
import com.example.voice.models.VoiceDiagnostics
import com.example.voice.models.VoiceProfileId
import com.example.voice.provider.AndroidSystemTtsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.Closeable

enum class CurrentScreen {
    MAIN,
    SETTINGS
}

/**
 * UI-level coordinator ViewModel.
 * Exclusively responsible for binding Compose UI to the central [SummerOrchestrator],
 * [VoiceEngineRouter], [VisionEngineRouter], [SpeechRecognitionRouter], and [EmbeddedModelManager].
 * Does NOT contain assistant reasoning, intent classification, or action authorization logic.
 */
class MainViewModel @JvmOverloads constructor(
    application: Application,
    val stateManager: SummerStateManager = SummerStateManager(),
    private val personality: SummerPersonality = SummerPersonality.DEFAULT,
    orchestratorInstance: SummerOrchestrator? = null,
    genAIStatusProvider: OnDeviceGenAIStatusProvider = AndroidOnDeviceGenAIStatusProvider(),
    visionEngineInstance: VisionEngine? = null,
    speechEngineInstance: SpeechRecognitionEngine? = null,
    embeddedModelManagerInstance: EmbeddedModelManager? = null,
    embeddedEngineInstance: AIEngine? = null
) : AndroidViewModel(application) {

    private val database = SummerDatabase.getInstance(application)
    val memoryRepository: MemoryRepository = LocalMemoryRepository(
        settingsDao = database.settingsDao(),
        memoryDao = database.memoryDao()
    )

    private val authorizationPolicy = DefaultActionAuthorizationPolicy()
    val actionExecutor = SecuredActionExecutor(authorizationPolicy)
    val networkProvider: NetworkInformationProvider = AndroidNetworkInformationProvider(application)

    // Phase 0I: Embedded Local Generative AI & Model Lifecycle Management
    val embeddedModelManager: EmbeddedModelManager = embeddedModelManagerInstance ?: DefaultEmbeddedModelManager(application)
    val embeddedEngine: AIEngine = embeddedEngineInstance ?: EmbeddedLocalAIEngine(
        context = application,
        modelManager = embeddedModelManager,
        personality = personality
    )

    // Phase 0C-R1 / Phase 0I: Local AI Capability, Registry & 3-Tier Routing Layer
    val aiModelRegistry = AIModelRegistry()
    val onDeviceGenAIProvider = OnDeviceGenAIProvider(statusProvider = genAIStatusProvider)
    val fallbackAIEngine = OfflineLocalAIEngine(personality)
    val deviceCapabilityDetector: DeviceAICapabilityDetector = AndroidDeviceAICapabilityDetector(
        context = application,
        networkProvider = networkProvider,
        genAIStatusProvider = genAIStatusProvider
    )

    val aiModelRouter = AIModelRouter(
        registry = aiModelRegistry,
        fallbackEngine = fallbackAIEngine,
        detector = deviceCapabilityDetector,
        networkProvider = networkProvider,
        embeddedModelManager = embeddedModelManager,
        embeddedEngine = embeddedEngine,
        routerScope = viewModelScope
    )

    val orchestrator: SummerOrchestrator = orchestratorInstance ?: SummerOrchestrator(
        stateManager = stateManager,
        sessionManager = SummerSessionManager(),
        eventBus = SummerEventBus(),
        aiEngine = aiModelRouter,
        decisionEngine = DefaultSummerDecisionEngine(),
        authorizationPolicy = authorizationPolicy,
        actionExecutor = actionExecutor,
        memoryRepository = memoryRepository,
        networkProvider = networkProvider
    )

    // Phase 0F: Voice Architecture & Natural Speech Foundation
    val systemTtsProvider = AndroidSystemTtsProvider(application)
    val voiceEngine = VoiceEngineRouter(
        systemTtsProvider = systemTtsProvider,
        eventBus = orchestrator.eventBus,
        scope = viewModelScope
    )

    // Phase 0G: Vision & Camera Architecture Foundation
    val visionCapabilityDetector = AndroidVisionCapabilityDetector(application)
    val visionEngine: VisionEngine = visionEngineInstance ?: VisionEngineRouter(
        detector = visionCapabilityDetector,
        eventBus = orchestrator.eventBus,
        routerScope = viewModelScope
    )

    // Phase 0H: Speech Recognition & Foreground Listening
    val speechEngine: SpeechRecognitionEngine = speechEngineInstance ?: AndroidSpeechRecognitionEngine(application)
    val speechRecognitionRouter = SpeechRecognitionRouter(
        engine = speechEngine,
        eventBus = orchestrator.eventBus,
        scope = viewModelScope
    )

    val speechRecognitionState: StateFlow<SpeechRecognitionState> = speechRecognitionRouter.state
    val partialSpeechTranscript: StateFlow<String> = speechRecognitionRouter.partialTranscript
    val speechDiagnostics: StateFlow<SpeechRecognitionDiagnostics> = speechRecognitionRouter.diagnostics

    val embeddedModelStatus: StateFlow<EmbeddedModelStatus> = embeddedModelManager.status
    val embeddedModelDiagnostics: StateFlow<EmbeddedModelDiagnostics> = embeddedModelManager.diagnostics
    val embeddedDownloadProgress: StateFlow<Float> = embeddedModelManager.downloadProgress

    // Observable states exposed to Compose UI
    val summerState: StateFlow<SummerState> = orchestrator.orchestratorState

    val currentInteraction: StateFlow<SummerInteraction?> = orchestrator.currentInteraction

    val interactionHistory: StateFlow<List<SummerInteraction>> = orchestrator.interactionHistory

    val settings: StateFlow<SummerSettings> = memoryRepository.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SummerSettings()
        )

    val networkState: StateFlow<NetworkState> = networkProvider.networkState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkProvider.getCurrentState()
        )

    val latestAuditLog: StateFlow<ActionAuditEntry?> = actionExecutor.auditLog
        .map { list -> list.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val recentResponse: StateFlow<SummerResponse?> = orchestrator.latestResponse

    val recentAssistantSpeech: StateFlow<String?> = orchestrator.latestResponse
        .map { it?.text }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Hello. I am ${personality.shortName}. All cognitive systems are active in local offline mode."
        )

    val voiceDiagnostics: StateFlow<VoiceDiagnostics> = voiceEngine.diagnostics
    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking

    val aiDiagnostics: StateFlow<AIDiagnostics> = combine(
        aiModelRouter.diagnostics,
        voiceEngine.diagnostics,
        visionEngine.diagnostics,
        speechRecognitionRouter.diagnostics
    ) { aiDiag, voiceDiag, visionDiag, speechDiag ->
        aiDiag.copy(
            voiceDiagnostics = voiceDiag,
            visionDiagnostics = visionDiag,
            speechDiagnostics = speechDiag
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AIDiagnostics(
            voiceDiagnostics = voiceEngine.diagnostics.value,
            visionDiagnostics = visionEngine.diagnostics.value,
            speechDiagnostics = speechRecognitionRouter.diagnostics.value
        )
    )

    private val _currentScreen = MutableStateFlow(CurrentScreen.MAIN)
    val currentScreen: StateFlow<CurrentScreen> = _currentScreen.asStateFlow()

    init {
        // Register the on-device GenAI provider in registry
        aiModelRegistry.register(
            providerType = AIProviderType.ON_DEVICE_GENAI,
            engine = onDeviceGenAIProvider,
            metadata = onDeviceGenAIProvider.metadata
        )

        // Observe voice settings and update voiceEngine parameters
        viewModelScope.launch {
            settings.collect { s ->
                val profileId = if (s.voiceProfileId.equals("MALE", ignoreCase = true)) {
                    VoiceProfileId.MALE
                } else {
                    VoiceProfileId.FEMALE
                }
                voiceEngine.selectVoiceProfile(profileId)
                voiceEngine.updateProfileSettings(
                    speechRate = s.speechSpeed,
                    pitch = s.speechPitch,
                    volume = s.speechVolume
                )
            }
        }
    }

    fun navigateTo(screen: CurrentScreen) {
        _currentScreen.value = screen
    }

    fun selectStateDemo(newState: SummerState) {
        stateManager.transitionTo(newState, cause = "User manual state demonstration")
    }

    fun updateSettings(newSettings: SummerSettings) {
        viewModelScope.launch {
            memoryRepository.updateSettings(newSettings)
        }
    }

    /**
     * Activates foreground speech recognition.
     * Cancels any active speech output, transitions to Listening state,
     * and delegates final transcript into the orchestrator pipeline.
     */
    fun startListening() {
        viewModelScope.launch {
            voiceEngine.stop()
            stateManager.transitionTo(SummerState.Listening(), cause = "User activated speech input")
            speechRecognitionRouter.startListening { transcript ->
                submitQuery(transcript, source = "voice.engine")
            }
        }
    }

    fun stopListening() {
        viewModelScope.launch {
            speechRecognitionRouter.stopListening()
        }
    }

    fun cancelListening() {
        speechRecognitionRouter.cancel()
    }

    /**
     * Submits user input to the cognitive orchestrator.
     * Enforces conversational interruptibility: active speech is immediately cancelled.
     */
    fun submitQuery(query: String, source: String = "ui.text_input") {
        viewModelScope.launch {
            // Conversational interruptibility
            voiceEngine.stop()

            val interaction = orchestrator.handleUserInput(input = query, source = source)
            val responseText = interaction.response?.text
            if (settings.value.voiceInteractionEnabled && !responseText.isNullOrBlank()) {
                voiceEngine.speak(responseText)
            }
        }
    }

    /**
     * Toggles speech recognition listening on user microphone tap.
     */
    fun triggerVoiceInteraction() {
        if (speechRecognitionState.value == SpeechRecognitionState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    /**
     * Re-runs device AI capability detection using official Prompt API and updates diagnostics.
     */
    fun refreshAICapabilities() {
        viewModelScope.launch {
            aiModelRouter.refreshCapabilities()
            voiceEngine.refreshDiagnostics()
        }
    }

    fun selectVoiceProfile(profileId: VoiceProfileId) {
        viewModelScope.launch {
            val currentSettings = memoryRepository.getSettings()
            updateSettings(currentSettings.copy(voiceProfileId = profileId.name))
        }
    }

    fun setSpeechSpeed(speed: Float) {
        viewModelScope.launch {
            val currentSettings = memoryRepository.getSettings()
            updateSettings(currentSettings.copy(speechSpeed = speed.coerceIn(0.5f, 2.0f)))
        }
    }

    fun previewVoice() {
        viewModelScope.launch {
            voiceEngine.stop()
            voiceEngine.speak("Hello. I am ${personality.shortName}. This is my active speech profile.")
        }
    }

    fun stopSpeech() {
        viewModelScope.launch {
            voiceEngine.stop()
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAllMemories()
        }
    }

    fun clearConversationHistory() {
        orchestrator.sessionManager.startNewSession()
    }

    /**
     * Installs the embedded local generative language model (Gemma 3 1B IT).
     */
    fun installEmbeddedModel() {
        viewModelScope.launch {
            embeddedModelManager.install()
            aiModelRouter.refreshCapabilities()
        }
    }

    /**
     * Cancels an ongoing embedded model download.
     */
    fun cancelEmbeddedModelDownload() {
        viewModelScope.launch {
            embeddedModelManager.cancelDownload()
        }
    }

    /**
     * Deletes the installed embedded local model and frees storage.
     * Preserves all user memories, settings, and conversation history.
     */
    fun deleteEmbeddedModel() {
        viewModelScope.launch {
            (embeddedEngine as? Closeable)?.close()
            embeddedModelManager.deleteInstalledModel()
            aiModelRouter.refreshCapabilities()
        }
    }

    fun getPersonality(): SummerPersonality = personality

    override fun onCleared() {
        super.onCleared()
        (embeddedEngine as? Closeable)?.close()
        voiceEngine.release()
        visionEngine.release()
        speechRecognitionRouter.release()
    }
}
