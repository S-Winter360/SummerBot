package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.actions.ActionAuditEntry
import com.example.actions.ActionRequest
import com.example.actions.ActionResult
import com.example.actions.SecuredActionExecutor
import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.models.AIRequest
import com.example.core.personality.SummerPersonality
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.memory.LocalMemoryRepository
import com.example.memory.MemoryRepository
import com.example.memory.database.SummerDatabase
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.network.AndroidNetworkInformationProvider
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import com.example.security.Capability
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.security.SecurityContext
import com.example.voice.StubVoiceEngine
import com.example.voice.VoiceEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CurrentScreen {
    MAIN,
    SETTINGS
}

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val stateManager: SummerStateManager = SummerStateManager(),
    private val personality: SummerPersonality = SummerPersonality.DEFAULT
) : AndroidViewModel(application) {

    private val database = SummerDatabase.getInstance(application)
    val memoryRepository: MemoryRepository = LocalMemoryRepository(
        settingsDao = database.settingsDao(),
        memoryDao = database.memoryDao()
    )

    private val authorizationPolicy = DefaultActionAuthorizationPolicy()
    val actionExecutor = SecuredActionExecutor(authorizationPolicy)

    val aiEngine: AIEngine = OfflineLocalAIEngine(personality)
    val voiceEngine: VoiceEngine = StubVoiceEngine()
    val networkProvider: NetworkInformationProvider = AndroidNetworkInformationProvider(application)

    // Observable states
    val summerState: StateFlow<SummerState> = stateManager.state

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

    val latestAuditLog: StateFlow<ActionAuditEntry?> = MutableStateFlow<ActionAuditEntry?>(null)
        .also { mutableFlow ->
            viewModelScope.launch {
                actionExecutor.auditLog.collect { list ->
                    mutableFlow.value = list.firstOrNull()
                }
            }
        }

    private val _currentScreen = MutableStateFlow(CurrentScreen.MAIN)
    val currentScreen: StateFlow<CurrentScreen> = _currentScreen.asStateFlow()

    private val _recentAssistantSpeech = MutableStateFlow<String?>(
        "Hello. I am ${personality.shortName}. All cognitive systems are active in local offline mode."
    )
    val recentAssistantSpeech: StateFlow<String?> = _recentAssistantSpeech.asStateFlow()

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

    fun submitQuery(query: String) {
        viewModelScope.launch {
            // Check master switch
            val currentSettings = settings.value
            if (!currentSettings.summerEnabled) {
                stateManager.transitionTo(SummerState.Error("Summer is currently disabled in settings."))
                _recentAssistantSpeech.value = "Summer is disabled. Enable it in settings to resume interactions."
                return@launch
            }

            // 1. Transition to Thinking
            stateManager.transitionTo(SummerState.Thinking("Processing intent offline..."))
            delay(500) // Brief natural pause for state progression

            // 2. Process query via AIEngine
            val aiResponse = aiEngine.processQuery(AIRequest(query = query))

            // 3. If action is required, process through capability gatekeeper
            val action = aiResponse.requiredAction
            if (action != null) {
                stateManager.transitionTo(SummerState.Executing(action.actionName))
                val context = SecurityContext(caller = "AI_QUERY_PIPELINE")
                val actionResult = actionExecutor.execute(action, context, currentSettings)

                when (actionResult) {
                    is ActionResult.Success -> {
                        _recentAssistantSpeech.value = "${aiResponse.text} ${actionResult.output}"
                    }
                    is ActionResult.Denied -> {
                        _recentAssistantSpeech.value = "Action request blocked by security policy: ${actionResult.reason}"
                    }
                    is ActionResult.PendingConsent -> {
                        _recentAssistantSpeech.value = "Action requires confirmation: ${actionResult.prompt}"
                    }
                    is ActionResult.Failed -> {
                        _recentAssistantSpeech.value = "Action execution failure: ${actionResult.error}"
                    }
                }
            } else {
                _recentAssistantSpeech.value = aiResponse.text
            }

            // 4. Save conversation record in local memory
            if (currentSettings.personalMemoryEnabled) {
                memoryRepository.recordMemory(
                    MemoryRecord(
                        category = MemoryCategory.CONVERSATION_MEMORY,
                        title = "Query: ${query.take(30)}",
                        content = "User: $query | Summer: ${_recentAssistantSpeech.value}"
                    )
                )
            }

            // 5. Speak response and return to Idle
            stateManager.transitionTo(SummerState.Speaking(_recentAssistantSpeech.value))
            delay(1200)
            stateManager.resetToIdle()
        }
    }

    fun triggerVoiceInteraction() {
        viewModelScope.launch {
            val currentSettings = settings.value

            // Capability check for microphone
            val auth = authorizationPolicy.evaluate(
                Capability.MICROPHONE,
                SecurityContext(),
                currentSettings
            )

            if (!auth.isAllowed) {
                actionExecutor.execute(
                    ActionRequest(
                        capability = Capability.MICROPHONE,
                        actionName = "Voice Stream Request",
                        reasoning = "Microphone button activated by user."
                    ),
                    SecurityContext(),
                    currentSettings
                )
                stateManager.transitionTo(SummerState.Error("Voice interaction disabled in settings."))
                _recentAssistantSpeech.value = "Voice interaction is blocked by your current security settings."
                delay(2000)
                stateManager.resetToIdle()
                return@launch
            }

            // Execute voice interaction state pipeline
            stateManager.transitionTo(SummerState.Listening("Foreground microphone active"))
            _recentAssistantSpeech.value = "Listening..."
            delay(1500)

            // Transition to thinking
            stateManager.transitionTo(SummerState.Thinking("Parsing audio waveform..."))
            delay(800)

            // Produce response
            val speechResponse = "Audio input captured. Voice processing architecture is verified and standing by."
            _recentAssistantSpeech.value = speechResponse
            stateManager.transitionTo(SummerState.Speaking(speechResponse))
            delay(1500)

            stateManager.resetToIdle()
        }
    }

    fun getPersonality(): SummerPersonality = personality
}
