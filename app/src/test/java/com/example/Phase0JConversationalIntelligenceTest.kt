package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.capability.AIAvailabilityStatus
import com.example.ai.capability.AICapability
import com.example.ai.capability.AIModelMetadata
import com.example.ai.capability.AIModelRegistry
import com.example.ai.capability.AIModelRouter
import com.example.ai.capability.AIProviderType
import com.example.ai.capability.GeminiResponseParser
import com.example.ai.capability.SummerPromptBuilder
import com.example.ai.localmodel.EmbeddedModelDiagnostics
import com.example.ai.localmodel.EmbeddedModelId
import com.example.ai.localmodel.EmbeddedModelManager
import com.example.ai.localmodel.EmbeddedModelMetadata
import com.example.ai.localmodel.EmbeddedModelStatus
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.decision.DefaultSummerDecisionEngine
import com.example.core.event.SummerEventBus
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.orchestrator.SummerOrchestrator
import com.example.core.personality.SummerPersonality
import com.example.core.response.MemoryOperation
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerStateManager
import com.example.memory.LocalMemoryRepository
import com.example.memory.database.SummerDatabase
import com.example.memory.models.ConversationRole
import com.example.memory.models.ConversationTurn
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryContext
import com.example.memory.models.MemoryRecord
import com.example.security.DefaultActionAuthorizationPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class Phase0JConversationalIntelligenceTest {

    private class TestEmbeddedModelManager(
        initialStatus: EmbeddedModelStatus = EmbeddedModelStatus.NOT_INSTALLED,
        private var ready: Boolean = false,
        private var path: String? = null
    ) : EmbeddedModelManager {
        private val _status = MutableStateFlow(initialStatus)
        override val status: StateFlow<EmbeddedModelStatus> = _status.asStateFlow()

        private val _diagnostics = MutableStateFlow(
            EmbeddedModelDiagnostics(
                status = initialStatus,
                modelName = "Gemma 3 1B IT INT4",
                activeBackend = "CPU",
                localModelFilePath = path
            )
        )
        override val diagnostics: StateFlow<EmbeddedModelDiagnostics> = _diagnostics.asStateFlow()

        private val _downloadProgress = MutableStateFlow(0f)
        override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

        fun setReady(isReady: Boolean, filePath: String = "/data/data/com.example/files/embedded_models/gemma-3-1b-it-cpu.litertlm") {
            ready = isReady
            val s = if (isReady) EmbeddedModelStatus.READY else EmbeddedModelStatus.NOT_INSTALLED
            _status.value = s
            path = if (isReady) filePath else null
            _diagnostics.value = _diagnostics.value.copy(status = s, localModelFilePath = path)
        }

        override suspend fun inspect(): EmbeddedModelStatus = _status.value
        override suspend fun importModel(uri: android.net.Uri): Result<Unit> = Result.success(Unit)
        override suspend fun importModelStream(
            inputStreamProvider: () -> InputStream?,
            fileNameHint: String?,
            totalSizeBytes: Long?
        ): Result<Unit> = Result.success(Unit)
        override suspend fun cancelImport() {}
        override suspend fun verify(): Result<Unit> = Result.success(Unit)
        override suspend fun deleteInstalledModel(): Result<Unit> {
            setReady(false)
            return Result.success(Unit)
        }
        override fun getInstalledModelPath(): String? = path
        override fun isModelReady(): Boolean = ready
    }

    private class CapturingAIEngine(
        override val modelInfo: AIModelInfo = AIModelInfo("Capturing Engine", "1.0", true, "Test"),
        override var isReady: Boolean = true,
        var cannedResponse: String = "This is a natural response."
    ) : AIEngine {
        var lastContextReceived: SummerContext? = null
        var lastInteractionReceived: SummerInteraction? = null
        var invocationCount = 0

        override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult {
            invocationCount++
            lastContextReceived = context
            lastInteractionReceived = interaction
            return AIResult(
                intent = SummerIntent.GeneralConversation(cannedResponse, rawQuery = interaction.userInput),
                response = SummerResponse(
                    text = cannedResponse,
                    type = ResponseType.TEXT,
                    source = modelInfo.name
                )
            )
        }

        override suspend fun classifyIntent(input: String): SummerIntent = SummerIntent.GeneralConversation(input)
        override suspend fun processQuery(request: AIRequest): AIResponse = AIResponse(cannedResponse, modelInfo.name, RecognizedIntent.GeneralConversation(request.query))
        override suspend fun evaluateIntent(input: String): RecognizedIntent = RecognizedIntent.GeneralConversation(input)
    }

    // 1. Provider availability: Gemini unavailable, Embedded ready -> effective local AI AVAILABLE -> Embedded selected
    @Test
    fun testProviderAvailability_GeminiUnavailable_EmbeddedReady() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val embeddedEngine = CapturingAIEngine(AIModelInfo("Gemma 3 1B IT INT4", "1.0", true, "LiteRT-LM"))
        val manager = TestEmbeddedModelManager()
        manager.setReady(true)

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = manager,
            embeddedEngine = embeddedEngine
        )

        // Gemini Nano is explicitly unavailable
        registry.register(
            AIProviderType.ON_DEVICE_GENAI,
            CapturingAIEngine(isReady = false),
            AIModelMetadata(
                provider = AIProviderType.ON_DEVICE_GENAI,
                modelIdentifier = "gemini-nano",
                displayName = "Gemini Nano",
                availabilityStatus = AIAvailabilityStatus.UNAVAILABLE
            )
        )

        router.refreshCapabilities()
        val diag = router.diagnostics.value

        // On-device GenAI is unavailable
        assertEquals(AIAvailabilityStatus.UNAVAILABLE, diag.onDeviceGenAIStatus)
        // Embedded model is ready
        assertEquals(EmbeddedModelStatus.READY, diag.embeddedModelStatus)
        // Effective Local AI is AVAILABLE
        assertEquals(AIAvailabilityStatus.AVAILABLE, diag.effectiveStatus)
        assertEquals(AIAvailabilityStatus.AVAILABLE, diag.runtimeStatus)
        // Active Provider is Embedded Local
        assertEquals(AIProviderType.EMBEDDED_LOCAL_MODEL, diag.activeProvider)
        assertEquals("Gemma 3 1B IT INT4", diag.currentModel)
        assertEquals("LiteRT-LM", diag.activeRuntime)
        assertEquals("INT4", diag.activeQuantization)
        assertEquals("Offline", diag.executionMode)
        assertFalse(diag.isFallbackActive)

        // Routing selects embedded engine
        val (selectedType, engine) = router.route(AICapability.CHAT)
        assertEquals(AIProviderType.EMBEDDED_LOCAL_MODEL, selectedType)
        assertEquals(embeddedEngine, engine)
    }

    // 2. Provider availability: Gemini AVAILABLE, Embedded READY -> Gemini preferred
    @Test
    fun testProviderAvailability_GeminiAvailable_PreferredOverEmbedded() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val geminiEngine = CapturingAIEngine(AIModelInfo("Gemini Nano", "1.0", true, "ML Kit"), isReady = true)
        val embeddedEngine = CapturingAIEngine(AIModelInfo("Gemma 3 1B IT INT4", "1.0", true, "LiteRT-LM"), isReady = true)
        val manager = TestEmbeddedModelManager()
        manager.setReady(true)

        registry.register(
            AIProviderType.ON_DEVICE_GENAI,
            geminiEngine,
            AIModelMetadata(
                provider = AIProviderType.ON_DEVICE_GENAI,
                modelIdentifier = "gemini-nano",
                displayName = "Gemini Nano",
                availabilityStatus = AIAvailabilityStatus.AVAILABLE,
                supportedCapabilities = setOf(AICapability.CHAT, AICapability.TEXT_GENERATION)
            )
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = manager,
            embeddedEngine = embeddedEngine
        )

        router.refreshCapabilities()
        val diag = router.diagnostics.value

        assertEquals(AIAvailabilityStatus.AVAILABLE, diag.onDeviceGenAIStatus)
        assertEquals(EmbeddedModelStatus.READY, diag.embeddedModelStatus)
        assertEquals(AIAvailabilityStatus.AVAILABLE, diag.effectiveStatus)
        assertEquals(AIProviderType.ON_DEVICE_GENAI, diag.activeProvider)

        val (selectedType, engine) = router.route(AICapability.CHAT)
        assertEquals(AIProviderType.ON_DEVICE_GENAI, selectedType)
        assertEquals(geminiEngine, engine)
    }

    // 3. Provider availability: Gemini unavailable, Embedded unavailable -> Deterministic selected
    @Test
    fun testProviderAvailability_BothUnavailable_DeterministicFallback() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val manager = TestEmbeddedModelManager()
        manager.setReady(false)

        registry.register(
            AIProviderType.ON_DEVICE_GENAI,
            CapturingAIEngine(isReady = false),
            AIModelMetadata(
                provider = AIProviderType.ON_DEVICE_GENAI,
                modelIdentifier = "gemini-nano",
                displayName = "Gemini Nano",
                availabilityStatus = AIAvailabilityStatus.UNAVAILABLE
            )
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = manager
        )

        router.refreshCapabilities()
        val diag = router.diagnostics.value

        assertEquals(AIAvailabilityStatus.UNAVAILABLE, diag.onDeviceGenAIStatus)
        assertEquals(EmbeddedModelStatus.NOT_INSTALLED, diag.embeddedModelStatus)
        assertEquals(AIProviderType.DETERMINISTIC_LOCAL, diag.activeProvider)
        assertEquals(fallback.modelInfo.name, diag.currentModel)
        assertTrue(diag.isFallbackActive)

        val (selectedType, engine) = router.route(AICapability.CHAT)
        assertEquals(AIProviderType.DETERMINISTIC_LOCAL, selectedType)
        assertEquals(fallback, engine)
    }

    // 4. Identity behaviour: ordinary conversation does not prepend self-introduction
    @Test
    fun testIdentityBehaviour_OrdinaryConversationDoesNotSelfIntroduce() {
        val promptBuilder = SummerPromptBuilder()
        val context = SummerContext(sessionId = "s1")
        val interaction = SummerInteraction(sessionId = "s1", userInput = "I'm tired today.")

        val prompt = promptBuilder.buildPrompt(context, interaction)
        assertTrue(prompt.contains("IDENTITY BEHAVIOUR"))
        assertTrue(prompt.contains("Do not introduce yourself at the beginning of every response"))
        assertTrue(prompt.contains("Do not repeatedly state that you are an AI assistant"))
        assertTrue(prompt.contains("Do not repeatedly describe your capabilities"))
        assertTrue(prompt.contains("I'm tired today."))

        // Verify parser does not force self-introduction on clean generative response
        val parser = GeminiResponseParser()
        val result = parser.parse(
            generatedText = "Sounds like you've had a rough day. What made it exhausting?",
            interaction = interaction
        )
        assertFalse(result.response.text.contains("I am Summer"))
        assertFalse(result.response.text.contains("How can I assist you"))
        assertEquals("Sounds like you've had a rough day. What made it exhausting?", result.response.text)
    }

    // 5. Identity question: "What is your name?" produces identity response
    @Test
    fun testIdentityQuestion_ProducesIdentityResponse() = runTest {
        val fallback = OfflineLocalAIEngine()
        val context = SummerContext(sessionId = "s1")
        val interaction = SummerInteraction(sessionId = "s1", userInput = "What is your name?")

        val result = fallback.process(context, interaction)
        assertTrue(result.intent is SummerIntent.IdentityQuestion)
        assertTrue(result.response.text.contains("Summer"))
    }

    // 6. Conversation continuity: recent conversation context reaches the AI engine
    @Test
    fun testConversationContinuity_RecentTurnsReachPrompt() {
        val promptBuilder = SummerPromptBuilder(maxRecentInteractions = 4)
        val turns = listOf(
            ConversationTurn(role = ConversationRole.USER, text = "I'm learning Python."),
            ConversationTurn(role = ConversationRole.ASSISTANT, text = "That's a good direction."),
            ConversationTurn(role = ConversationRole.USER, text = "What should I learn first?"),
            ConversationTurn(role = ConversationRole.ASSISTANT, text = "Start with variables and basic data types.")
        )
        val context = SummerContext(
            sessionId = "s1",
            conversationTurns = turns
        )
        val interaction = SummerInteraction(sessionId = "s1", userInput = "And after that?")

        val prompt = promptBuilder.buildPrompt(context, interaction)
        assertTrue(prompt.contains("User: I'm learning Python."))
        assertTrue(prompt.contains("Summer: That's a good direction."))
        assertTrue(prompt.contains("User: What should I learn first?"))
        assertTrue(prompt.contains("Summer: Start with variables and basic data types."))
        assertTrue(prompt.contains("User: And after that?"))
        assertTrue(prompt.contains("CONVERSATION & PERSONALITY DIRECTIVES"))
        assertTrue(prompt.contains("continuity"))
    }

    // 7. Memory behaviour: explicit memory commands persist via validated pipeline
    @Test
    fun testMemoryBehaviour_ExplicitMemoryCommandPersists() = runTest {
        val context: Application = ApplicationProvider.getApplicationContext()
        val database = SummerDatabase.getInstance(context)
        val memoryRepo = LocalMemoryRepository(database.settingsDao(), database.memoryDao())

        val parser = GeminiResponseParser()
        val interaction = SummerInteraction(sessionId = "s1", userInput = "Remember that I prefer Python over Java.")
        val operations = parser.extractMemoryOperations(interaction.userInput, interaction.userInput.lowercase())

        assertEquals(1, operations.size)
        assertTrue(operations.first() is MemoryOperation.Store)
        val storeOp = operations.first() as MemoryOperation.Store
        assertTrue(storeOp.record.content.contains("prefer Python over Java", ignoreCase = true))

        val validation = com.example.memory.validation.MemoryOperationValidator().validate(storeOp)
        assertTrue(validation is com.example.memory.validation.MemoryValidationResult.Valid)

        memoryRepo.recordMemory(storeOp.record)
        val active = memoryRepo.getActiveMemories()
        assertTrue(active.any { it.content.contains("prefer Python over Java", ignoreCase = true) })
    }

    // 8. No automatic memory pollution: normal conversation creates NO persistent memories
    @Test
    fun testNoAutomaticMemoryPollution_NormalConversationCreatesNoMemories() {
        val parser = GeminiResponseParser()
        val normalStatements = listOf(
            "I'm tired today.",
            "Python looks interesting today.",
            "I had a really long day today.",
            "What should I work on this afternoon?",
            "Do you remember what I told you yesterday?",
            "I'm thinking about learning Python.",
            "That response doesn't make sense.",
            "Tell me something interesting.",
            "What do you think?"
        )

        for (statement in normalStatements) {
            val ops = parser.extractMemoryOperations(statement, statement.lowercase())
            assertTrue("Statement '$statement' must not create any memory operation", ops.isEmpty())
        }
    }

    // 9. User corrections: natural correction updates preference memory
    @Test
    fun testUserCorrection_UpdatesPreferenceMemory() = runTest {
        val context: Application = ApplicationProvider.getApplicationContext()
        val database = SummerDatabase.getInstance(context)
        val memoryRepo = LocalMemoryRepository(database.settingsDao(), database.memoryDao())

        // Initial preference
        memoryRepo.recordMemory(
            MemoryRecord(
                category = MemoryCategory.USER_PREFERENCE,
                title = "Language Preference",
                content = "My favourite language is Java",
                confidence = 1.0f
            )
        )

        val parser = GeminiResponseParser()
        val correctionInput = "My favourite language isn't Java. It's Python."
        val ops = parser.extractMemoryOperations(correctionInput, correctionInput.lowercase())

        assertEquals(1, ops.size)
        assertTrue(ops.first() is MemoryOperation.Update)
        val updateOp = ops.first() as MemoryOperation.Update
        assertEquals("Java", updateOp.previousContent)
        assertTrue(updateOp.record.content.contains("Python", ignoreCase = true))

        // Validate and apply update
        val validator = com.example.memory.validation.MemoryOperationValidator()
        val valResult = validator.validate(updateOp)
        assertTrue(valResult is com.example.memory.validation.MemoryValidationResult.Valid)

        val existing = memoryRepo.getActiveMemories().first { it.content.contains("Java") }
        memoryRepo.updateMemory(updateOp.record.copy(id = existing.id))

        val updated = memoryRepo.getActiveMemories()
        assertTrue(updated.any { it.content.contains("Python") })
    }

    // 10. Settings diagnostics report Embedded Local AI = AVAILABLE when model installed
    @Test
    fun testSettingsDiagnostics_ReportsEmbeddedAvailableWhenInstalled() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val manager = TestEmbeddedModelManager()
        manager.setReady(true)

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = manager,
            embeddedEngine = CapturingAIEngine(AIModelInfo("Gemma 3 1B IT INT4", "1.0", true, "LiteRT-LM"))
        )

        router.refreshCapabilities()
        val diag = router.diagnostics.value

        assertEquals(AIAvailabilityStatus.AVAILABLE, diag.effectiveStatus)
        assertEquals(AIAvailabilityStatus.AVAILABLE, diag.runtimeStatus)
        assertEquals(EmbeddedModelStatus.READY, diag.embeddedModelStatus)
        assertEquals(AIProviderType.EMBEDDED_LOCAL_MODEL, diag.activeProvider)
        assertEquals("Gemma 3 1B IT INT4", diag.currentModel)
        assertEquals("LiteRT-LM", diag.activeRuntime)
        assertEquals("INT4", diag.activeQuantization)
        assertEquals("Offline", diag.executionMode)
    }
}
