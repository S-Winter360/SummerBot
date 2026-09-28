package com.example.core.orchestrator

import android.util.Log
import com.example.actions.ActionAuditEntry
import com.example.actions.ActionExecutor
import com.example.actions.ActionResult
import com.example.ai.AIEngine
import com.example.core.context.SummerContext
import com.example.core.decision.SummerDecisionEngine
import com.example.core.event.EventPriority
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import com.example.core.interaction.InteractionState
import com.example.core.interaction.SummerInteraction
import com.example.core.response.MemoryOperation
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.core.session.SummerSession
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.memory.MemoryRepository
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.network.NetworkInformationProvider
import com.example.security.ActionAuthorizationPolicy
import com.example.security.SecurityContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object SummerLog {
    fun i(tag: String, msg: String) {
        try {
            android.util.Log.i(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }
    fun d(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            System.err.println("[$tag] $msg: ${tr?.message}")
        }
    }
}

/**
 * Summer's central cognitive orchestrator.
 * Coordinates perception, context formulation, AI parsing, decision generation,
 * capability authorization, action execution, and memory persistence.
 */
class SummerOrchestrator(
    val stateManager: SummerStateManager,
    val sessionManager: SummerSessionManager,
    val eventBus: SummerEventBus,
    val aiEngine: AIEngine,
    val decisionEngine: SummerDecisionEngine,
    val authorizationPolicy: ActionAuthorizationPolicy,
    val actionExecutor: ActionExecutor,
    val memoryRepository: MemoryRepository,
    val networkProvider: NetworkInformationProvider,
    private val orchestratorScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        private const val TAG = "SummerOrchestrator"
    }

    // Observable states
    val orchestratorState: StateFlow<SummerState> = stateManager.state

    private val _currentInteraction = MutableStateFlow<SummerInteraction?>(null)
    val currentInteraction: StateFlow<SummerInteraction?> = _currentInteraction.asStateFlow()

    private val _interactionHistory = MutableStateFlow<List<SummerInteraction>>(emptyList())
    val interactionHistory: StateFlow<List<SummerInteraction>> = _interactionHistory.asStateFlow()

    private val _latestResponse = MutableStateFlow<SummerResponse?>(
        SummerResponse(
            text = "Hello. I am Summer. All cognitive systems are active in local offline mode.",
            type = ResponseType.INFORMATION,
            source = "Summer Initialization"
        )
    )
    val latestResponse: StateFlow<SummerResponse?> = _latestResponse.asStateFlow()

    /**
     * Primary entry point for natural language input from the user.
     * Enforces the cognitive pipeline:
     * User Input → Event → Context → AI → Decision → Authorization → Action → Memory → Response
     */
    suspend fun handleUserInput(
        input: String,
        source: String = "ui.text_input"
    ): SummerInteraction {
        val trimmedInput = input.trim()
        val session = sessionManager.currentSession.value
        val sessionId = session.id

        // Guard against empty input
        if (trimmedInput.isEmpty()) {
            val emptyInteraction = SummerInteraction(
                sessionId = sessionId,
                inputSource = source,
                userInput = "",
                processingState = InteractionState.FAILED,
                errorMessage = "Input was empty"
            )
            return emptyInteraction
        }

        // 1. Initial Interaction created (RECEIVED)
        var interaction = SummerInteraction(
            sessionId = sessionId,
            inputSource = source,
            userInput = trimmedInput,
            processingState = InteractionState.RECEIVED
        )
        _currentInteraction.value = interaction
        sessionManager.recordInteraction(interaction)

        SummerLog.i(TAG, "Interaction received: id=${interaction.id} source=$source")

        // 2. Publish event to event bus
        eventBus.publish(
            SummerEvent.UserTextInput(
                text = trimmedInput,
                sessionId = sessionId,
                source = source,
                priority = EventPriority.NORMAL
            )
        )

        try {
            // 3. Transition to Thinking (UNDERSTANDING)
            interaction = interaction.copy(processingState = InteractionState.UNDERSTANDING)
            _currentInteraction.value = interaction
            stateManager.transitionTo(SummerState.Thinking("Parsing intent and context..."), cause = "Query processing")

            val currentSettings = memoryRepository.getSettings()
            val currentNetwork = networkProvider.getCurrentState()

            // 4. Build Context snapshot (REASONING)
            interaction = interaction.copy(processingState = InteractionState.REASONING)
            _currentInteraction.value = interaction

            val context = SummerContext(
                sessionId = sessionId,
                recentConversation = session.interactions.takeLast(5),
                networkState = currentNetwork,
                currentState = stateManager.state.value,
                settings = currentSettings
            )

            // 5. AI Engine processes context + interaction
            val aiResult = aiEngine.process(context, interaction)
            SummerLog.i(TAG, "Intent identified: ${aiResult.intent::class.java.simpleName} confidence=${aiResult.confidence}")

            // 6. Decision Engine produces decision
            val decision = decisionEngine.decide(aiResult, context)
            SummerLog.i(TAG, "Decision generated: requiresConfirmation=${decision.requiresConfirmation} actions=${decision.actionRequests.size}")

            var finalResponse = decision.response
            val actionResults = mutableListOf<ActionResult>()

            // 7. If action requested, process through security authorization & executor
            if (decision.actionRequests.isNotEmpty()) {
                interaction = interaction.copy(
                    processingState = InteractionState.EXECUTING,
                    actionRequests = decision.actionRequests
                )
                _currentInteraction.value = interaction

                for (action in decision.actionRequests) {
                    stateManager.transitionTo(
                        SummerState.Executing(action.actionName),
                        cause = "Action execution [${action.actionName}]"
                    )

                    SummerLog.i(TAG, "Action requested: [${action.actionName}] cap=${action.capability.name}")

                    val secContext = SecurityContext(
                        caller = "SUMMER_DECISION_ENGINE",
                        isUserInitiated = true
                    )

                    val actionResult = actionExecutor.execute(action, secContext, currentSettings)
                    actionResults.add(actionResult)

                    eventBus.publish(
                        SummerEvent.ActionResultEvent(
                            actionId = action.id,
                            result = actionResult,
                            sessionId = sessionId
                        )
                    )

                    SummerLog.i(TAG, "Action result: isSuccess=${actionResult.isSuccess} type=${actionResult::class.java.simpleName}")

                    // Update response message based on action outcome
                    finalResponse = when (actionResult) {
                        is ActionResult.Success -> {
                            finalResponse.copy(
                                text = "${finalResponse.text} ${actionResult.output}"
                            )
                        }
                        is ActionResult.Denied -> {
                            finalResponse.copy(
                                text = "Action [${action.actionName}] blocked by security policy: ${actionResult.reason}",
                                type = ResponseType.ERROR
                            )
                        }
                        is ActionResult.PendingConsent -> {
                            finalResponse.copy(
                                text = "Action [${action.actionName}] requires confirmation: ${actionResult.prompt}",
                                type = ResponseType.CONFIRMATION
                            )
                        }
                        is ActionResult.Failed -> {
                            finalResponse.copy(
                                text = "Action [${action.actionName}] failed: ${actionResult.error}",
                                type = ResponseType.ERROR
                            )
                        }
                    }
                }
            }

            // 8. Execute memory operations if any
            if (decision.memoryOperations.isNotEmpty()) {
                stateManager.transitionTo(SummerState.Learning("Updating local memory"), cause = "Memory persistence")
                for (op in decision.memoryOperations) {
                    when (op) {
                        is MemoryOperation.Store -> {
                            memoryRepository.recordMemory(op.record)
                            SummerLog.i(TAG, "Memory stored: category=${op.record.category} title=${op.record.title}")
                        }
                        is MemoryOperation.Forget -> {
                            // Deferred to memory phase
                        }
                        is MemoryOperation.Update -> {
                            memoryRepository.recordMemory(op.record)
                        }
                    }
                }
            }

            // 9. Store conversation in local memory (distinguishing conversation memory from learned memory)
            if (currentSettings.personalMemoryEnabled) {
                memoryRepository.recordMemory(
                    MemoryRecord(
                        category = MemoryCategory.CONVERSATION_MEMORY,
                        title = "Query: ${trimmedInput.take(30)}",
                        content = "User: $trimmedInput | Summer: ${finalResponse.text}"
                    )
                )
            }

            // 10. Responding state & speaking transition
            interaction = interaction.copy(
                processingState = InteractionState.RESPONDING,
                response = finalResponse
            )
            _currentInteraction.value = interaction
            _latestResponse.value = finalResponse

            SummerLog.i(TAG, "Response generated: type=${finalResponse.type} textLength=${finalResponse.text.length}")

            stateManager.transitionTo(
                SummerState.Speaking(finalResponse.text),
                cause = "Delivering response"
            )

            // Pacing delay for natural feedback before returning to idle
            delay(1200)

            // 11. Completion
            val completedInteraction = interaction.copy(
                processingState = InteractionState.COMPLETED,
                completedTimestamp = System.currentTimeMillis()
            )
            _currentInteraction.value = completedInteraction
            _interactionHistory.update { (listOf(completedInteraction) + it).take(50) }
            sessionManager.recordInteraction(completedInteraction)

            stateManager.resetToIdle(cause = "Interaction completed")
            return completedInteraction

        } catch (e: Exception) {
            SummerLog.e(TAG, "Error in orchestration pipeline", e)

            val errorResponse = SummerResponse(
                text = "An internal processing error occurred: ${e.message ?: "Unknown fault"}",
                type = ResponseType.ERROR,
                confidence = 0f,
                source = "Summer Fault Recovery"
            )

            val failedInteraction = interaction.copy(
                processingState = InteractionState.FAILED,
                response = errorResponse,
                errorMessage = e.message,
                completedTimestamp = System.currentTimeMillis()
            )

            _currentInteraction.value = failedInteraction
            _latestResponse.value = errorResponse
            _interactionHistory.update { (listOf(failedInteraction) + it).take(50) }
            sessionManager.recordInteraction(failedInteraction)

            stateManager.transitionTo(
                SummerState.Error(e.message ?: "Unknown cognitive processing failure"),
                cause = "Pipeline exception"
            )
            delay(1500)
            stateManager.resetToIdle(cause = "Recovery from error")

            return failedInteraction
        }
    }

    /**
     * Generic event handler for system and sensor events entering Summer.
     */
    suspend fun handleEvent(event: SummerEvent): SummerInteraction? {
        return when (event) {
            is SummerEvent.UserTextInput -> {
                handleUserInput(event.text, source = event.source)
            }
            is SummerEvent.NetworkStateChanged -> {
                SummerLog.i(TAG, "Observed network change: ${event.newState}")
                null
            }
            is SummerEvent.SystemEvent -> {
                SummerLog.i(TAG, "System event: ${event.eventName} - ${event.details}")
                null
            }
            is SummerEvent.VoiceInput -> {
                handleUserInput(event.transcript, source = event.source)
            }
            else -> {
                SummerLog.d(TAG, "Unhandled event type: ${event::class.java.simpleName}")
                null
            }
        }
    }
}
