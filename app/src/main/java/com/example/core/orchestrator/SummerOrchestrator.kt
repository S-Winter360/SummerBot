package com.example.core.orchestrator

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
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.w(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] $msg: ${tr?.message}")
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

    suspend fun handleUserInput(input: String, source: String = "ui.text_input"): SummerInteraction {
        val session = sessionManager.getActiveSession()
        val interactionId = java.util.UUID.randomUUID().toString()

        var interaction = SummerInteraction(
            id = interactionId,
            sessionId = session.id,
            userInput = input,
            state = InteractionState.RECEIVED
        )
        _currentInteraction.value = interaction
        sessionManager.recordInteraction(interaction)

        SummerLog.i(TAG, "Interaction received: id=${interaction.id} source=$source")

        eventBus.publish(
            SummerEvent.UserTextInput(
                text = input,
                source = source,
                sessionId = session.id,
                priority = EventPriority.NORMAL
            )
        )

        try {
            stateManager.transitionTo(SummerState.Thinking(input), cause = "Understanding user input")
            interaction = interaction.copy(state = InteractionState.UNDERSTANDING)
            _currentInteraction.value = interaction

            val settings = memoryRepository.getSettings()
            val netState = networkProvider.getCurrentState()

            val context = SummerContext(
                sessionId = session.id,
                recentInteractions = session.interactions.takeLast(5),
                networkState = netState,
                currentState = stateManager.state.value,
                settings = settings
            )

            interaction = interaction.copy(state = InteractionState.REASONING)
            _currentInteraction.value = interaction

            val aiResult = aiEngine.process(context, interaction)
            SummerLog.i(TAG, "Intent identified: ${aiResult.intent::class.java.simpleName} confidence=${aiResult.confidence}")

            val decision = decisionEngine.decide(aiResult, context)
            SummerLog.i(TAG, "Decision generated: requiresConfirmation=${decision.requiresConfirmation} actions=${decision.actionRequests.size}")

            var finalResponse = decision.response
            val actionResults = mutableListOf<ActionResult>()

            if (decision.actionRequests.isNotEmpty()) {
                interaction = interaction.copy(state = InteractionState.EXECUTING)
                _currentInteraction.value = interaction

                for (action in decision.actionRequests) {
                    stateManager.transitionTo(
                        SummerState.Executing(action.actionName),
                        cause = "Action execution [${action.actionName}]"
                    )

                    SummerLog.i(TAG, "Action requested: [${action.actionName}] cap=${action.capability.name}")

                    val secContext = SecurityContext(
                        caller = "SUMMER_DECISION_ENGINE",
                        isUserInitiated = true,
                        sessionAuthorized = true
                    )

                    val actionResult = actionExecutor.execute(action, secContext)
                    actionResults.add(actionResult)

                    eventBus.publish(
                        SummerEvent.ActionResultEvent(
                            actionName = action.actionName,
                            result = actionResult,
                            sessionId = session.id
                        )
                    )

                    SummerLog.i(TAG, "Action result: isSuccess=${actionResult.isSuccess} type=${actionResult::class.java.simpleName}")

                    finalResponse = when (actionResult) {
                        is ActionResult.Success -> {
                            finalResponse.copy(
                                text = "${finalResponse.text}\nExecution outcome: ${actionResult.message}"
                            )
                        }
                        is ActionResult.Denied -> {
                            finalResponse.copy(
                                text = "${finalResponse.text}\nExecution denied: ${actionResult.message}",
                                type = ResponseType.ERROR
                            )
                        }
                        is ActionResult.Failure -> {
                            finalResponse.copy(
                                text = "${finalResponse.text}\nExecution fault: ${actionResult.message}",
                                type = ResponseType.ERROR
                            )
                        }
                    }
                }
            }

            if (settings.personalMemoryEnabled) {
                stateManager.transitionTo(SummerState.Learning("Interaction consolidation"), cause = "Memory persistence")

                memoryRepository.recordMemory(
                    MemoryRecord(
                        category = MemoryCategory.CONVERSATION,
                        title = "User Input",
                        content = input
                    )
                )

                for (op in decision.memoryOperations) {
                    when (op) {
                        is MemoryOperation.Store -> {
                            memoryRepository.recordMemory(op.record)
                            SummerLog.i(TAG, "Memory stored: category=${op.record.category} title=${op.record.title}")
                        }
                        is MemoryOperation.Forget -> {}
                        is MemoryOperation.Update -> {
                            memoryRepository.recordMemory(op.record)
                        }
                    }
                }
            }

            interaction = interaction.copy(
                state = InteractionState.RESPONDING,
                response = finalResponse
            )
            _currentInteraction.value = interaction
            _latestResponse.value = finalResponse

            SummerLog.i(TAG, "Response generated: type=${finalResponse.type} textLength=${finalResponse.text.length}")

            stateManager.transitionTo(
                SummerState.Speaking(finalResponse.text),
                cause = "Delivering response to user"
            )

            val completedInteraction = interaction.copy(
                state = InteractionState.COMPLETED,
                completionTimestamp = System.currentTimeMillis()
            )
            _currentInteraction.value = completedInteraction
            _interactionHistory.update { listOf(completedInteraction) + it.take(49) }
            sessionManager.recordInteraction(completedInteraction)

            stateManager.transitionTo(SummerState.Idle, cause = "Interaction sequence completed")

            return completedInteraction

        } catch (e: Exception) {
            SummerLog.e(TAG, "Error in orchestration pipeline", e)

            val errorResponse = SummerResponse(
                text = "An internal processing error occurred: ${e.message ?: "Unknown fault"}",
                type = ResponseType.ERROR,
                source = "Orchestrator Safeguard"
            )

            stateManager.transitionTo(
                SummerState.Error(e.message ?: "Internal fault"),
                cause = "Exception caught in orchestration pipeline"
            )

            val failedInteraction = interaction.copy(
                state = InteractionState.FAILED,
                response = errorResponse,
                completionTimestamp = System.currentTimeMillis()
            )

            _currentInteraction.value = failedInteraction
            _latestResponse.value = errorResponse
            _interactionHistory.update { listOf(failedInteraction) + it.take(49) }

            stateManager.transitionTo(SummerState.Idle, cause = "Recovered to idle state")

            return failedInteraction
        }
    }
}
