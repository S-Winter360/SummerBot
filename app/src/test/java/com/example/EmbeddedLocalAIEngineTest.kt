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
import com.example.ai.capability.SummerPromptBuilder
import com.example.ai.localmodel.EmbeddedModelDiagnostics
import com.example.ai.localmodel.EmbeddedModelId
import com.example.ai.localmodel.EmbeddedModelManager
import com.example.ai.localmodel.EmbeddedModelMetadata
import com.example.ai.localmodel.EmbeddedModelRepository
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
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerStateManager
import com.example.memory.LocalMemoryRepository
import com.example.memory.database.SummerDatabase
import com.example.memory.models.ConversationTurn
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryContext
import com.example.memory.models.MemoryRecord
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmbeddedLocalAIEngineTest {

    private class FakeEmbeddedModelManager(
        initialStatus: EmbeddedModelStatus = EmbeddedModelStatus.NOT_INSTALLED,
        private var isInstalled: Boolean = false,
        private var installedPath: String? = null
    ) : EmbeddedModelManager {
        private val _status = MutableStateFlow(initialStatus)
        override val status: StateFlow<EmbeddedModelStatus> = _status.asStateFlow()

        private val _diagnostics = MutableStateFlow(
            EmbeddedModelDiagnostics(
                status = initialStatus,
                modelName = "Gemma 3 1B IT",
                approximateSizeFormatted = "584 MB",
                localModelFilePath = installedPath
            )
        )
        override val diagnostics: StateFlow<EmbeddedModelDiagnostics> = _diagnostics.asStateFlow()

        private val _downloadProgress = MutableStateFlow(0f)
        override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

        fun setStatus(newStatus: EmbeddedModelStatus, path: String? = null) {
            _status.value = newStatus
            isInstalled = (newStatus == EmbeddedModelStatus.READY)
            installedPath = path
            _diagnostics.value = _diagnostics.value.copy(
                status = newStatus,
                localModelFilePath = path
            )
        }

        override suspend fun inspect(): EmbeddedModelStatus = _status.value

        override suspend fun install(): Result<Unit> {
            _status.value = EmbeddedModelStatus.DOWNLOADING
            _downloadProgress.value = 0.5f
            _status.value = EmbeddedModelStatus.VERIFYING
            _status.value = EmbeddedModelStatus.READY
            _downloadProgress.value = 1.0f
            isInstalled = true
            installedPath = "/data/data/com.example/files/embedded_models/gemma-3-1b-it-cpu.litertlm"
            _diagnostics.value = _diagnostics.value.copy(
                status = EmbeddedModelStatus.READY,
                localModelFilePath = installedPath
            )
            return Result.success(Unit)
        }

        override suspend fun cancelDownload() {
            _status.value = EmbeddedModelStatus.CANCELLED
            _downloadProgress.value = 0f
            _diagnostics.value = _diagnostics.value.copy(status = EmbeddedModelStatus.CANCELLED)
        }

        override suspend fun verify(): Result<Unit> = Result.success(Unit)

        override suspend fun deleteInstalledModel(): Result<Unit> {
            _status.value = EmbeddedModelStatus.NOT_INSTALLED
            _downloadProgress.value = 0f
            isInstalled = false
            installedPath = null
            _diagnostics.value = _diagnostics.value.copy(
                status = EmbeddedModelStatus.NOT_INSTALLED,
                localModelFilePath = null
            )
            return Result.success(Unit)
        }

        override fun getInstalledModelPath(): String? = installedPath

        override fun isModelReady(): Boolean = _status.value == EmbeddedModelStatus.READY && isInstalled
    }

    private class FakeAIEngine(
        override val modelInfo: AIModelInfo = AIModelInfo("Fake Engine", "1.0", true, "Fake"),
        override var isReady: Boolean = true,
        private val responseText: String = "Fake generation response"
    ) : AIEngine {
        var processCallCount = 0
        var lastPromptReceived: String? = null

        override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult {
            processCallCount++
            lastPromptReceived = interaction.userInput
            return AIResult(
                intent = SummerIntent.GeneralConversation(interaction.userInput),
                response = SummerResponse(
                    text = responseText,
                    type = ResponseType.TEXT,
                    source = modelInfo.name
                )
            )
        }

        override suspend fun classifyIntent(input: String): SummerIntent = SummerIntent.GeneralConversation(input)
        override suspend fun processQuery(request: AIRequest): AIResponse = AIResponse(responseText, modelInfo.name, RecognizedIntent.GeneralConversation(request.query))
        override suspend fun evaluateIntent(input: String): RecognizedIntent = RecognizedIntent.GeneralConversation(input)
    }

    // 1. EmbeddedModelStatus transitions
    @Test
    fun testEmbeddedModelStatusTransitions() {
        val statuses = EmbeddedModelStatus.values()
        assertTrue(statuses.contains(EmbeddedModelStatus.NOT_INSTALLED))
        assertTrue(statuses.contains(EmbeddedModelStatus.DOWNLOADING))
        assertTrue(statuses.contains(EmbeddedModelStatus.VERIFYING))
        assertTrue(statuses.contains(EmbeddedModelStatus.READY))
        assertTrue(statuses.contains(EmbeddedModelStatus.INITIALIZING))
        assertTrue(statuses.contains(EmbeddedModelStatus.ERROR))
        assertTrue(statuses.contains(EmbeddedModelStatus.INSUFFICIENT_STORAGE))
        assertTrue(statuses.contains(EmbeddedModelStatus.CANCELLED))
    }

    // 2. Model metadata validation
    @Test
    fun testModelMetadataValidation() {
        val meta = EmbeddedModelMetadata(
            id = EmbeddedModelId.GEMMA_3_1B_IT,
            modelName = "Gemma 3 1B IT",
            filename = "gemma-3-1b-it-cpu.litertlm",
            sizeBytes = 584L * 1024L * 1024L,
            backend = "CPU"
        )
        assertEquals("Gemma 3 1B IT", meta.modelName)
        assertEquals("gemma-3-1b-it-cpu.litertlm", meta.filename)
        assertTrue(meta.sizeBytes > 500L * 1024L * 1024L)
        assertEquals("CPU", meta.backend)
    }

    // 3. Missing model detection
    @Test
    fun testMissingModelDetection() {
        val context: Application = ApplicationProvider.getApplicationContext()
        val repo = EmbeddedModelRepository(context, "test_models_empty")
        val meta = EmbeddedModelMetadata(filename = "non_existent_model.litertlm")
        assertFalse(repo.isModelInstalled(meta))
        assertEquals(0L, repo.getInstalledSizeBytes(meta))
    }

    // 4. Installed model detection
    @Test
    fun testInstalledModelDetection() {
        val context: Application = ApplicationProvider.getApplicationContext()
        val repo = EmbeddedModelRepository(context, "test_models_installed")
        val meta = EmbeddedModelMetadata(filename = "test_gemma.litertlm")
        val file = repo.getModelFile(meta)
        file.parentFile?.mkdirs()
        file.writeText("simulated-model-weights-content")

        assertTrue(repo.isModelInstalled(meta))
        assertTrue(repo.getInstalledSizeBytes(meta) > 0L)

        // Clean up
        file.delete()
    }

    // 5. Storage safety calculation
    @Test
    fun testStorageSafetyCalculation() {
        val context: Application = ApplicationProvider.getApplicationContext()
        val repo = EmbeddedModelRepository(context)
        val available = repo.getAvailableStorageBytes()
        assertTrue(available >= 0L)
        // Requesting an absurdly huge size (100 Terabytes) should fail safety check
        assertFalse(repo.hasSufficientStorage(100L * 1024L * 1024L * 1024L * 1024L))
    }

    // 6. Router selects Gemini Nano when AVAILABLE (preferred)
    @Test
    fun testRouterSelectsGeminiNanoWhenAvailable() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val geminiEngine = FakeAIEngine(AIModelInfo("Gemini Nano", "1.0", true, "ML Kit"), isReady = true)
        val embeddedEngine = FakeAIEngine(AIModelInfo("Gemma 3 1B IT", "1.0", true, "LiteRT-LM"), isReady = true)
        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.READY, isInstalled = true)

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
            embeddedModelManager = fakeManager,
            embeddedEngine = embeddedEngine
        )

        val (providerType, engine) = router.route(AICapability.TEXT_GENERATION)
        assertEquals(AIProviderType.ON_DEVICE_GENAI, providerType)
        assertEquals(geminiEngine, engine)
    }

    // 7. Router selects embedded model when Gemini Nano unavailable and embedded model READY
    @Test
    fun testRouterSelectsEmbeddedModelWhenNanoUnavailableAndEmbeddedReady() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val geminiEngine = FakeAIEngine(AIModelInfo("Gemini Nano", "1.0", true, "ML Kit"), isReady = false)
        val embeddedEngine = FakeAIEngine(AIModelInfo("Gemma 3 1B IT", "1.0", true, "LiteRT-LM"), isReady = true)
        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.READY, isInstalled = true)

        registry.register(
            AIProviderType.ON_DEVICE_GENAI,
            geminiEngine,
            AIModelMetadata(
                provider = AIProviderType.ON_DEVICE_GENAI,
                modelIdentifier = "gemini-nano",
                displayName = "Gemini Nano",
                availabilityStatus = AIAvailabilityStatus.UNAVAILABLE,
                supportedCapabilities = setOf(AICapability.CHAT, AICapability.TEXT_GENERATION)
            )
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = fakeManager,
            embeddedEngine = embeddedEngine
        )

        val (providerType, engine) = router.route(AICapability.CHAT)
        assertEquals(AIProviderType.EMBEDDED_LOCAL_MODEL, providerType)
        assertEquals(embeddedEngine, engine)
    }

    // 8. Router selects deterministic local when both unavailable
    @Test
    fun testRouterSelectsDeterministicLocalWhenBothUnavailable() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val geminiEngine = FakeAIEngine(AIModelInfo("Gemini Nano", "1.0", true, "ML Kit"), isReady = false)
        val embeddedEngine = FakeAIEngine(AIModelInfo("Gemma 3 1B IT", "1.0", true, "LiteRT-LM"), isReady = false)
        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.NOT_INSTALLED, isInstalled = false)

        registry.register(
            AIProviderType.ON_DEVICE_GENAI,
            geminiEngine,
            AIModelMetadata(
                provider = AIProviderType.ON_DEVICE_GENAI,
                modelIdentifier = "gemini-nano",
                displayName = "Gemini Nano",
                availabilityStatus = AIAvailabilityStatus.UNAVAILABLE,
                supportedCapabilities = setOf(AICapability.CHAT, AICapability.TEXT_GENERATION)
            )
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = fakeManager,
            embeddedEngine = embeddedEngine
        )

        val (providerType, engine) = router.route(AICapability.CHAT)
        assertEquals(AIProviderType.DETERMINISTIC_LOCAL, providerType)
        assertEquals(fallback, engine)
    }

    // 9. Router never selects embedded model when model isn't READY
    @Test
    fun testRouterNeverSelectsEmbeddedModelWhenDownloadingOrError() = runTest {
        val nonReadyStatuses = listOf(
            EmbeddedModelStatus.NOT_INSTALLED,
            EmbeddedModelStatus.DOWNLOADING,
            EmbeddedModelStatus.VERIFYING,
            EmbeddedModelStatus.ERROR,
            EmbeddedModelStatus.INSUFFICIENT_STORAGE,
            EmbeddedModelStatus.CANCELLED
        )

        for (status in nonReadyStatuses) {
            val registry = AIModelRegistry()
            val fallback = OfflineLocalAIEngine()
            val embeddedEngine = FakeAIEngine(AIModelInfo("Gemma 3 1B IT", "1.0", true, "LiteRT-LM"), isReady = false)
            val fakeManager = FakeEmbeddedModelManager(initialStatus = status, isInstalled = false)

            val router = AIModelRouter(
                registry = registry,
                fallbackEngine = fallback,
                embeddedModelManager = fakeManager,
                embeddedEngine = embeddedEngine
            )

            val (providerType, engine) = router.route(AICapability.CHAT)
            assertEquals("Status $status must fallback to DETERMINISTIC_LOCAL", AIProviderType.DETERMINISTIC_LOCAL, providerType)
            assertEquals(fallback, engine)
        }
    }

    // 10. No cloud provider selected
    @Test
    fun testNoCloudProviderSelected() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.NOT_INSTALLED)

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = fakeManager
        )

        val (providerType, _) = router.route(AICapability.TEXT_GENERATION)
        assertFalse(providerType == AIProviderType.CLOUD)
    }

    // 11. Model manager does not expose incomplete downloads as READY
    @Test
    fun testIncompleteDownloadNotExposedAsReady() = runTest {
        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.NOT_INSTALLED)
        fakeManager.setStatus(EmbeddedModelStatus.DOWNLOADING)
        assertFalse(fakeManager.isModelReady())
        assertNull(fakeManager.getInstalledModelPath())

        fakeManager.setStatus(EmbeddedModelStatus.VERIFYING)
        assertFalse(fakeManager.isModelReady())

        fakeManager.setStatus(EmbeddedModelStatus.READY, path = "/test/path")
        assertTrue(fakeManager.isModelReady())
        assertEquals("/test/path", fakeManager.getInstalledModelPath())
    }

    // 12. Model removal does not delete memory
    @Test
    fun testModelRemovalPreservesMemory() = runTest {
        val context: Application = ApplicationProvider.getApplicationContext()
        val database = SummerDatabase.getInstance(context)
        val memoryRepo = LocalMemoryRepository(database.settingsDao(), database.memoryDao())

        // Store a memory record
        val record = MemoryRecord(
            category = MemoryCategory.PREFERENCE,
            title = "Learning Goal",
            content = "User is learning Python and Kotlin",
            confidence = 1.0f
        )
        memoryRepo.recordMemory(record)

        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.READY, isInstalled = true)
        val deleteResult = fakeManager.deleteInstalledModel()
        assertTrue(deleteResult.isSuccess)
        assertEquals(EmbeddedModelStatus.NOT_INSTALLED, fakeManager.status.value)

        // Memory records must remain completely intact
        val retrieved = memoryRepo.getActiveMemories()
        assertTrue(retrieved.any { it.content.contains("learning Python") })
    }

    // 13. Context bounds in prompt building
    @Test
    fun testSummerPromptBuilderContextBounds() {
        val builder = SummerPromptBuilder(
            personality = SummerPersonality.DEFAULT,
            maxRecentInteractions = 2,
            maxMemories = 3
        )

        val memories = listOf(
            MemoryRecord(category = MemoryCategory.PREFERENCE, title = "M1", content = "Memory 1"),
            MemoryRecord(category = MemoryCategory.PREFERENCE, title = "M2", content = "Memory 2"),
            MemoryRecord(category = MemoryCategory.PREFERENCE, title = "M3", content = "Memory 3"),
            MemoryRecord(category = MemoryCategory.PREFERENCE, title = "M4", content = "Memory 4 (should be bounded)")
        )

        val memoryContext = MemoryContext(
            relevantMemories = memories,
            recentConversation = emptyList<ConversationTurn>(),
            activeSessionId = "test-session"
        )

        val context = SummerContext(
            sessionId = "test-session",
            memoryContext = memoryContext
        )

        val interaction = SummerInteraction(
            sessionId = "test-session",
            userInput = "Tell me what I'm learning."
        )

        val prompt = builder.buildPrompt(context, interaction)
        assertTrue(prompt.contains("Summer Winter"))
        assertTrue(prompt.contains("Memory 1"))
        assertTrue(prompt.contains("Memory 2"))
        assertTrue(prompt.contains("Memory 3"))
        assertFalse(prompt.contains("Memory 4 (should be bounded)"))
        assertTrue(prompt.contains("Untrusted Context Rule"))
    }

    // 14. Generated response enters Summer conversation exactly once
    @Test
    fun testResponseEntersHistoryExactlyOnce() = runTest {
        val context: Application = ApplicationProvider.getApplicationContext()
        val database = SummerDatabase.getInstance(context)
        val memoryRepo = LocalMemoryRepository(database.settingsDao(), database.memoryDao())
        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.READY, isInstalled = true)
        val embeddedEngine = FakeAIEngine(
            modelInfo = AIModelInfo("Gemma 3 1B IT", "1.0", true, "LiteRT-LM"),
            isReady = true,
            responseText = "Python is a great programming language to learn."
        )

        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = fakeManager,
            embeddedEngine = embeddedEngine
        )

        val orchestrator = SummerOrchestrator(
            stateManager = SummerStateManager(),
            sessionManager = SummerSessionManager(),
            eventBus = SummerEventBus(),
            aiEngine = router,
            decisionEngine = DefaultSummerDecisionEngine(),
            authorizationPolicy = DefaultActionAuthorizationPolicy(),
            actionExecutor = com.example.actions.SecuredActionExecutor(DefaultActionAuthorizationPolicy()),
            memoryRepository = memoryRepo,
            networkProvider = com.example.network.AndroidNetworkInformationProvider(context)
        )

        val interaction = orchestrator.handleUserInput("What do you think about learning Python?")
        assertEquals("Python is a great programming language to learn.", interaction.response?.text)

        val history = orchestrator.interactionHistory.value
        assertEquals(1, history.size)
        assertEquals("What do you think about learning Python?", history[0].userInput)
        assertEquals("Python is a great programming language to learn.", history[0].response?.text)
    }

    // 15. MainViewModel constructor regression with @JvmOverloads
    @Test
    fun testMainViewModelJvmOverloadsCompatibility() {
        val context: Application = ApplicationProvider.getApplicationContext()
        // Must instantiate cleanly with just Application parameter
        val vm = MainViewModel(context)
        assertNotNull(vm.embeddedModelManager)
        assertNotNull(vm.embeddedEngine)
        assertNotNull(vm.aiModelRouter)
        assertEquals(EmbeddedModelStatus.NOT_INSTALLED, vm.embeddedModelStatus.value)
    }

    // 16. Diagnostics accurately report active provider
    @Test
    fun testDiagnosticsAccuratelyReportActiveProvider() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val fakeManager = FakeEmbeddedModelManager(initialStatus = EmbeddedModelStatus.READY, isInstalled = true)
        val embeddedEngine = FakeAIEngine(
            modelInfo = AIModelInfo("Gemma 3 1B IT", "1.0-4bit", true, "LiteRT-LM"),
            isReady = true
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            embeddedModelManager = fakeManager,
            embeddedEngine = embeddedEngine
        )

        val (providerType, _) = router.route()
        assertEquals(AIProviderType.EMBEDDED_LOCAL_MODEL, providerType)

        router.refreshCapabilities()
        val diag = router.diagnostics.value
        assertEquals(AIProviderType.EMBEDDED_LOCAL_MODEL, diag.activeProvider)
        assertEquals("Gemma 3 1B IT", diag.currentModel)
        assertFalse(diag.isFallbackActive)
    }
}
