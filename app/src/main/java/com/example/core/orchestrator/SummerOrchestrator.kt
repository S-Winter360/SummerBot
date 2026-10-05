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
import com.example.memory.models.ConversationRole
import com.example.memory.models.ConversationTurn
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryContext
import com.example.memory.models.MemoryRecord
import com.example.memory.retrieval.DefaultMemoryRetriever
import com.example.memory.retrieval.MemoryRetriever
import com.example.memory.validation.MemoryOperationValidator
import com.example.memory.validation.MemoryValidationResult
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
    val memoryRetriever: MemoryRetriever = DefaultMemoryRetriever(),
    val memoryOperationValidator: MemoryOperationValidator = MemoryOperationValidator(),
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
            text = "Hello. I'm here whenever you'd like to talk.",
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

            // 1. Retrieve Candidate Memories from Local Persistent Store
            val allActiveMemories = if (settings.personalMemoryEnabled) {
                try {
                    memoryRepository.getActiveMemories()
                } catch (t: Throwable) {
                    SummerLog.w(TAG, "Failed to load active memories", t)
                    emptyList()
                }
            } else {
                emptyList()
            }

            // 2. Select Relevant Bounded Memories using Local Scoring
            val relevantMemories = if (allActiveMemories.isNotEmpty()) {
                memoryRetriever.retrieveRelevantMemories(
                    query = input,
                    allMemories = allActiveMemories,
                    maxResults = 5
                )
            } else {
                emptyList()
            }

            // 3. Map Short-Term Session Dialogue into Bounded Conversation Turns
            val recentTurns = session.interactions.takeLast(6).flatMap { past ->
                val turns = mutableListOf<ConversationTurn>()
                turns.add(ConversationTurn(role = ConversationRole.USER, text = past.userInput, timestamp = past.timestamp))
                if (past.response != null) {
                    turns.add(ConversationTurn(role = ConversationRole.ASSISTANT, text = past.response.text, timestamp = past.response.timestamp))
                }
                turns
            }

            val memoryContext = MemoryContext(
                relevantMemories = relevantMemories,
                recentConversation = recentTurns,
                activeSessionId = session.id,
                retrievalMetadata = mapOf("retrievedCount" to relevantMemories.size.toString())
            )

            val context = SummerContext(
                sessionId = session.id,
                recentInteractions = session.interactions.takeLast(5),
                conversationTurns = recentTurns,
                activeMemories = relevantMemories,
                memoryContext = memoryContext,
                networkState = netState,
                currentState = stateManager.state.value,
                settings = settings
            )

            interaction = interaction.copy(
                state = InteractionState.REASONING,
                referencedMemories = relevantMemories
            )
            _currentInteraction.value = interaction

            // 4. Invoke AI Engine (Gemini Nano if AVAILABLE, or OfflineLocalAIEngine fallback)
            val aiResult = aiEngine.process(context, interaction)
            SummerLog.i(TAG, "Intent identified: ${aiResult.intent::class.java.simpleName} confidence=${aiResult.confidence}")

            // 5. Synthesize Decision
            val decision = decisionEngine.decide(aiResult, context)
            SummerLog.i(TAG, "Decision generated: requiresConfirmation=${decision.requiresConfirmation} actions=${decision.actionRequests.size} memoryOps=${decision.memoryOperations.size}")

            var finalResponse = decision.response
            val actionResults = mutableListOf<ActionResult>()

            // 6. Execute Approved Actions Through Security Pipeline
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

            // 7. Validate & Persist Explicit Memory Operations
            if (settings.personalMemoryEnabled && decision.memoryOperations.isNotEmpty()) {
                stateManager.transitionTo(SummerState.Learning("Memory consolidation"), cause = "Explicit memory operation")

                for (op in decision.memoryOperations) {
                    val validation = memoryOperationValidator.validate(op)
                    if (validation is MemoryValidationResult.Valid) {
                        when (val validOp = validation.operation) {
                            is MemoryOperation.Store -> {
                                memoryRepository.recordMemory(validOp.record)
                                SummerLog.i(TAG, "Explicit memory stored: category=${validOp.record.category} content=${validOp.record.content}")
                            }
                            is MemoryOperation.Update -> {
                                if (validOp.targetId != null) {
                                    memoryRepository.updateMemory(validOp.record.copy(id = validOp.targetId))
                                } else {
                                    // Match against existing memories by content or title
                                    val match = allActiveMemories.firstOrNull {
                                        it.content.contains(validOp.previousContent ?: "", ignoreCase = true) ||
                                            it.title.contains(validOp.previousContent ?: "", ignoreCase = true)
                                    }
                                    if (match != null) {
                                        memoryRepository.updateMemory(validOp.record.copy(id = match.id))
                                    } else {
                                        memoryRepository.recordMemory(validOp.record)
                                    }
                                }
                                SummerLog.i(TAG, "Memory updated: content=${validOp.record.content}")
                            }
                            is MemoryOperation.Forget -> {
                                val deleted = memoryRepository.forgetMemory(
                                    targetId = validOp.targetId,
                                    keyword = validOp.keywordOrContent
                                )
                                SummerLog.i(TAG, "Memory forget performed (success=$deleted): target=${validOp.keywordOrContent ?: validOp.targetId}")
                            }
                            is MemoryOperation.ClearAll -> {
                                memoryRepository.clearAllMemories()
                                SummerLog.i(TAG, "All memories cleared via operation")
                            }
                        }
                    } else if (validation is MemoryValidationResult.Invalid) {
                        SummerLog.w(TAG, "Memory operation rejected by validator: ${validation.reason}")
                    }
                }
            }

            // 8. Update Memory Access Metadata for Retrieved Records (Non-blocking)
            if (relevantMemories.isNotEmpty()) {
                try {
                    for (mem in relevantMemories) {
                        memoryRepository.updateAccessMetadata(
                            id = mem.id,
                            count = mem.accessCount + 1,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                } catch (t: Throwable) {
                    SummerLog.w(TAG, "Error updating memory access metadata", t)
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
