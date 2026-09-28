package com.example

import com.example.actions.ActionAuditEntry
import com.example.actions.ActionExecutor
import com.example.actions.ActionRequest
import com.example.actions.ActionResult
import com.example.actions.SecuredActionExecutor
import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.decision.DefaultSummerDecisionEngine
import com.example.core.decision.SummerDecision
import com.example.core.event.EventPriority
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import com.example.core.interaction.InteractionState
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.orchestrator.SummerOrchestrator
import com.example.core.personality.SummerPersonality
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.core.session.SummerSession
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.memory.MemoryRepository
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import com.example.security.ActionAuthorizationPolicy
import com.example.security.Capability
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.security.SecurityContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive test suite validating the complete Summer cognitive architecture:
 * Events, Sessions, Interaction Lifecycle, Intent Parsing, AI Engine, Decision Engine,
 * Orchestrator Pipeline, Security Gates, Memory Persistence, and Error Recovery.
 */
class SummerOrchestratorTest {

    // --- Fake Test Implementations ---

    class FakeMemoryRepository(
        initialSettings: SummerSettings = SummerSettings()
    ) : MemoryRepository {
        val settingsFlow = MutableStateFlow(initialSettings)
        val storedMemories = mutableListOf<MemoryRecord>()

        override fun observeSettings(): Flow<SummerSettings> = settingsFlow
        override suspend fun getSettings(): SummerSettings = settingsFlow.value
        override suspend fun updateSettings(settings: SummerSettings) {
            settingsFlow.value = settings
        }

        override fun observeRecentMemories(limit: Int): Flow<List<MemoryRecord>> {
            return MutableStateFlow(storedMemories.takeLast(limit))
        }

        override suspend fun recordMemory(record: MemoryRecord): Long {
            storedMemories.add(record)
            return storedMemories.size.toLong()
        }

        override suspend fun searchMemories(query: String): List<MemoryRecord> {
            return storedMemories.filter { it.content.contains(query, ignoreCase = true) }
        }
    }

    class FakeNetworkProvider(
        initialState: NetworkState = NetworkState.OFFLINE
    ) : NetworkInformationProvider {
        private val _state = MutableStateFlow(initialState)
        override val networkState: Flow<NetworkState> = _state
        override val isOnline: Flow<Boolean> = _state.map { it != NetworkState.OFFLINE }
        override fun getCurrentState(): NetworkState = _state.value
    }

    class FakeActionExecutor(
        var shouldSucceed: Boolean = true,
        var mockOutput: String = "Simulated action output."
    ) : ActionExecutor {
        val executedRequests = mutableListOf<ActionRequest>()

        override suspend fun execute(
            request: ActionRequest,
            context: SecurityContext,
            settings: SummerSettings
        ): ActionResult {
            executedRequests.add(request)
            return if (shouldSucceed) {
                ActionResult.Success(mockOutput)
            } else {
                ActionResult.Failed("Simulated action failure.")
            }
        }
    }

    private fun createTestOrchestrator(
        settings: SummerSettings = SummerSettings(),
        customExecutor: ActionExecutor? = null,
        customAIEngine: AIEngine? = null,
        customPolicy: ActionAuthorizationPolicy = DefaultActionAuthorizationPolicy(),
        memoryRepo: FakeMemoryRepository = FakeMemoryRepository(settings)
    ): Pair<SummerOrchestrator, FakeMemoryRepository> {
        val stateManager = SummerStateManager()
        val sessionManager = SummerSessionManager()
        val eventBus = SummerEventBus()
        val aiEngine = customAIEngine ?: OfflineLocalAIEngine()
        val decisionEngine = DefaultSummerDecisionEngine()
        val executor = customExecutor ?: SecuredActionExecutor(customPolicy)
        val networkProvider = FakeNetworkProvider()

        val orchestrator = SummerOrchestrator(
            stateManager = stateManager,
            sessionManager = sessionManager,
            eventBus = eventBus,
            aiEngine = aiEngine,
            decisionEngine = decisionEngine,
            authorizationPolicy = customPolicy,
            actionExecutor = executor,
            memoryRepository = memoryRepo,
            networkProvider = networkProvider
        )
        return Pair(orchestrator, memoryRepo)
    }

    // 1. SummerEvent creation
    @Test
    fun testSummerEventCreation() {
        val textEvent = SummerEvent.UserTextInput(
            text = "Hello Summer",
            sessionId = "session-123",
            priority = EventPriority.HIGH
        )
        assertEquals("Hello Summer", textEvent.text)
        assertEquals("session-123", textEvent.sessionId)
        assertEquals(EventPriority.HIGH, textEvent.priority)
        assertEquals("ui.text_input", textEvent.source)
        assertNotNull(textEvent.id)
        assertTrue(textEvent.timestamp > 0)

        val netEvent = SummerEvent.NetworkStateChanged(
            newState = NetworkState.WIFI,
            sessionId = "session-123"
        )
        assertEquals(NetworkState.WIFI, netEvent.newState)

        val sysEvent = SummerEvent.SystemEvent(
            eventName = "BOOT_COMPLETE",
            details = "Cognitive core ready",
            sessionId = "session-123"
        )
        assertEquals("BOOT_COMPLETE", sysEvent.eventName)
    }

    // 2. SummerSession creation & lifecycle
    @Test
    fun testSummerSessionCreation() {
        val sessionManager = SummerSessionManager()
        val session = sessionManager.currentSession.value

        assertNotNull(session.id)
        assertTrue(session.isActive)
        assertEquals(0, session.interactionCount)

        val interaction = SummerInteraction(
            sessionId = session.id,
            userInput = "Test interaction"
        )
        sessionManager.recordInteraction(interaction)

        assertEquals(1, sessionManager.currentSession.value.interactionCount)
        assertEquals("Test interaction", sessionManager.currentSession.value.latestInteraction?.userInput)

        val newSession = sessionManager.startNewSession()
        assertTrue(newSession.isActive)
        assertEquals(0, newSession.interactionCount)
        assertEquals(1, sessionManager.pastSessions.value.size)
        assertFalse(sessionManager.pastSessions.value.first().isActive)
    }

    // 3. Interaction lifecycle states
    @Test
    fun testInteractionLifecycle() {
        var interaction = SummerInteraction(
            sessionId = "session-1",
            userInput = "Lifecycle test",
            processingState = InteractionState.RECEIVED
        )
        assertEquals(InteractionState.RECEIVED, interaction.processingState)

        interaction = interaction.copy(processingState = InteractionState.UNDERSTANDING)
        assertEquals(InteractionState.UNDERSTANDING, interaction.processingState)

        interaction = interaction.copy(processingState = InteractionState.REASONING)
        assertEquals(InteractionState.REASONING, interaction.processingState)

        interaction = interaction.copy(processingState = InteractionState.RESPONDING)
        assertEquals(InteractionState.RESPONDING, interaction.processingState)

        interaction = interaction.copy(processingState = InteractionState.COMPLETED)
        assertTrue(interaction.isCompleted)
        assertFalse(interaction.isFailed)
    }

    // 4. Deterministic intent classification
    @Test
    fun testDeterministicIntentClassification() = runTest {
        val aiEngine = OfflineLocalAIEngine()

        val greeting = aiEngine.classifyIntent("Hello Summer")
        assertTrue(greeting is SummerIntent.Greeting)

        val identity = aiEngine.classifyIntent("What is your name?")
        assertTrue(identity is SummerIntent.IdentityQuestion)

        val capability = aiEngine.classifyIntent("What can you do?")
        assertTrue(capability is SummerIntent.CapabilityQuestion)

        val time = aiEngine.classifyIntent("What time is it?")
        assertTrue(time is SummerIntent.TimeQuery)

        val netAction = aiEngine.classifyIntent("test network connection")
        assertTrue(netAction is SummerIntent.CapabilityAction)
        assertEquals(Capability.INTERNET, (netAction as SummerIntent.CapabilityAction).actionRequest.capability)

        val unknown = aiEngine.classifyIntent("xyz qwerty totally unknown phrase 987")
        assertTrue(unknown is SummerIntent.Unknown)
    }

    // 5. AIEngine result generation
    @Test
    fun testAIEngineResultGeneration() = runTest {
        val aiEngine = OfflineLocalAIEngine()
        val context = SummerContext(sessionId = "s-1")
        val interaction = SummerInteraction(sessionId = "s-1", userInput = "hello")

        val result = aiEngine.process(context, interaction)
        assertTrue(result.intent is SummerIntent.Greeting)
        assertEquals("Hello. I'm Summer. How can I assist you?", result.response.text)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    // 6. DecisionEngine behaviour
    @Test
    fun testDecisionEngineBehaviour() = runTest {
        val decisionEngine = DefaultSummerDecisionEngine()
        val context = SummerContext(sessionId = "s-1", settings = SummerSettings(summerEnabled = true))

        val dummyResponse = SummerResponse(text = "Sample response")
        val aiResult = AIResult(
            intent = SummerIntent.Greeting("hi"),
            response = dummyResponse,
            confidence = 1.0f
        )

        val decision = decisionEngine.decide(aiResult, context)
        assertEquals("Sample response", decision.response.text)
        assertFalse(decision.requiresConfirmation)
        assertEquals("Greeting", decision.metadata["intent"])

        // When Summer is disabled
        val disabledContext = SummerContext(sessionId = "s-1", settings = SummerSettings(summerEnabled = false))
        val disabledDecision = decisionEngine.decide(aiResult, disabledContext)
        assertEquals(ResponseType.ERROR, disabledDecision.response.type)
        assertTrue(disabledDecision.response.text.contains("disabled"))
    }

    // 7. Orchestrator text interaction
    @Test
    fun testOrchestratorTextInteraction() = runTest {
        val (orchestrator, _) = createTestOrchestrator()

        val interaction = orchestrator.handleUserInput("Hello Summer")
        assertEquals(InteractionState.COMPLETED, interaction.processingState)
        assertEquals("Hello. I'm Summer. How can I assist you?", interaction.response?.text)
        assertEquals(SummerState.Idle, orchestrator.orchestratorState.value)
    }

    // 8. Unknown input handling
    @Test
    fun testUnknownInputHandling() = runTest {
        val (orchestrator, _) = createTestOrchestrator()

        val interaction = orchestrator.handleUserInput("xyz random non-intent phrase")
        assertEquals(InteractionState.COMPLETED, interaction.processingState)
        assertEquals(
            "I don't have enough understanding for that yet. My local intelligence module is still being developed.",
            interaction.response?.text
        )
    }

    // 9. Action authorization integration
    @Test
    fun testActionAuthorizationIntegration() = runTest {
        // Internet disabled by default
        val (orchestrator, _) = createTestOrchestrator(
            settings = SummerSettings(internetAccessAllowed = false)
        )

        val interaction = orchestrator.handleUserInput("test network")
        assertEquals(InteractionState.COMPLETED, interaction.processingState)
        assertTrue(interaction.response?.text?.contains("blocked by security policy") == true)
        assertEquals(ResponseType.ERROR, interaction.response?.type)
    }

    // 10. Denied action handling
    @Test
    fun testDeniedActionHandling() = runTest {
        // Microphone access disabled
        val (orchestrator, _) = createTestOrchestrator(
            settings = SummerSettings(voiceInteractionEnabled = false)
        )

        val interaction = orchestrator.handleUserInput("test mic")
        assertEquals(InteractionState.COMPLETED, interaction.processingState)
        assertTrue(interaction.response?.text?.contains("blocked by security policy") == true)
        assertEquals(SummerState.Idle, orchestrator.orchestratorState.value)
    }

    // 11. Successful action pipeline using test executor
    @Test
    fun testSuccessfulActionPipelineWithTestExecutor() = runTest {
        val fakeExecutor = FakeActionExecutor(
            shouldSucceed = true,
            mockOutput = "Network ping verified offline isolation."
        )

        val (orchestrator, _) = createTestOrchestrator(
            settings = SummerSettings(internetAccessAllowed = true),
            customExecutor = fakeExecutor
        )

        val interaction = orchestrator.handleUserInput("test network")
        assertEquals(InteractionState.COMPLETED, interaction.processingState)
        assertEquals(1, fakeExecutor.executedRequests.size)
        assertEquals(Capability.INTERNET, fakeExecutor.executedRequests.first().capability)
        assertTrue(interaction.response?.text?.contains("Network ping verified offline isolation.") == true)
    }

    // 12. Memory interaction persistence
    @Test
    fun testMemoryInteractionPersistence() = runTest {
        val (orchestrator, memoryRepo) = createTestOrchestrator(
            settings = SummerSettings(personalMemoryEnabled = true)
        )

        orchestrator.handleUserInput("What is your name?")
        assertTrue(memoryRepo.storedMemories.isNotEmpty())
        val convoMem = memoryRepo.storedMemories.find { it.category == MemoryCategory.CONVERSATION_MEMORY }
        assertNotNull(convoMem)
        assertTrue(convoMem!!.content.contains("Summer Winter"))

        // Explicit fact memory
        orchestrator.handleUserInput("remember user prefers tea")
        val factMem = memoryRepo.storedMemories.find { it.category == MemoryCategory.LEARNED_FACT }
        assertNotNull(factMem)
        assertTrue(factMem!!.content.contains("user prefers tea"))
    }

    // 13. Error recovery in orchestrator
    @Test
    fun testErrorRecoveryInOrchestrator() = runTest {
        val faultyAIEngine = object : AIEngine {
            override val modelInfo: AIModelInfo = AIModelInfo("Faulty", "1.0", true, "Faulty engine")
            override val isReady: Boolean = true

            override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult {
                throw IllegalStateException("Simulated cognitive pipeline error")
            }

            override suspend fun classifyIntent(input: String): SummerIntent = SummerIntent.Unknown(input)
            override suspend fun processQuery(request: AIRequest): AIResponse = throw UnsupportedOperationException()
            override suspend fun evaluateIntent(input: String): RecognizedIntent = throw UnsupportedOperationException()
        }

        val (orchestrator, _) = createTestOrchestrator(customAIEngine = faultyAIEngine)

        val interaction = orchestrator.handleUserInput("Trigger crash")
        assertEquals(InteractionState.FAILED, interaction.processingState)
        assertEquals(ResponseType.ERROR, interaction.response?.type)
        assertTrue(interaction.response?.text?.contains("Simulated cognitive pipeline error") == true)
        // Verify state gracefully recovers to Idle
        assertEquals(SummerState.Idle, orchestrator.orchestratorState.value)
    }

    // 14. Identity and capability questions
    @Test
    fun testIdentityAndCapabilityQuestions() = runTest {
        val (orchestrator, _) = createTestOrchestrator()

        val idInteraction = orchestrator.handleUserInput("What is your name?")
        assertEquals("I'm Summer Winter. You can call me Summer.", idInteraction.response?.text)

        val capInteraction = orchestrator.handleUserInput("What can you do?")
        assertTrue(capInteraction.response?.text?.contains("architecture is being prepared") == true)

        val timeInteraction = orchestrator.handleUserInput("What time is it?")
        assertTrue(timeInteraction.response?.text?.contains("It is currently") == true)
    }
}
