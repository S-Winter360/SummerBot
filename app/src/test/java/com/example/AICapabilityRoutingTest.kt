package com.example

import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.capability.AIAvailabilityStatus
import com.example.ai.capability.AICapability
import com.example.ai.capability.AIDiagnostics
import com.example.ai.capability.AIModelMetadata
import com.example.ai.capability.AIModelRegistry
import com.example.ai.capability.AIModelRouter
import com.example.ai.capability.AIProviderType
import com.example.ai.capability.DeviceAICapabilityDetector
import com.example.ai.capability.DeviceAIProfile
import com.example.ai.capability.OnDeviceGenAIProvider
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for Phase 0C: Local AI Engine Strategy & Device Capability Detection.
 */
class AICapabilityRoutingTest {

    private class FakeNetworkProvider(
        initialState: NetworkState = NetworkState.OFFLINE
    ) : NetworkInformationProvider {
        private val _state = MutableStateFlow(initialState)
        override val networkState: Flow<NetworkState> = _state
        override val isOnline: Flow<Boolean> = _state.map { it != NetworkState.OFFLINE }
        override fun getCurrentState(): NetworkState = _state.value
        fun setState(state: NetworkState) { _state.value = state }
    }

    private class FakeCapabilityDetector(
        var profileToReturn: DeviceAIProfile = DeviceAIProfile(
            apiLevel = 35,
            manufacturer = "Google",
            model = "Pixel 9",
            onDeviceGenAIAvailability = AIAvailabilityStatus.NOT_SUPPORTED,
            recommendedProvider = AIProviderType.DETERMINISTIC_LOCAL
        ),
        var shouldThrow: Boolean = false
    ) : DeviceAICapabilityDetector {
        var detectCallCount = 0
        override suspend fun detect(): DeviceAIProfile {
            detectCallCount++
            if (shouldThrow) {
                throw IllegalStateException("Hardware capability sensor fault")
            }
            return profileToReturn
        }
    }

    // 1. AI Provider Enum definitions
    @Test
    fun testAIProviderTypeEnum() {
        assertEquals("On-Device GenAI", AIProviderType.ON_DEVICE_GENAI.displayName)
        assertEquals("Embedded Local Model", AIProviderType.EMBEDDED_LOCAL_MODEL.displayName)
        assertEquals("Deterministic Local", AIProviderType.DETERMINISTIC_LOCAL.displayName)
        assertEquals("Cloud AI", AIProviderType.CLOUD.displayName)
    }

    // 2. AI Availability States
    @Test
    fun testAIAvailabilityStatus() {
        assertEquals("Checking...", AIAvailabilityStatus.CHECKING.label)
        assertEquals("Available", AIAvailabilityStatus.AVAILABLE.label)
        assertEquals("Downloadable", AIAvailabilityStatus.DOWNLOADABLE.label)
        assertEquals("Unavailable", AIAvailabilityStatus.UNAVAILABLE.label)
        assertEquals("Not Supported", AIAvailabilityStatus.NOT_SUPPORTED.label)
        assertEquals("Error", AIAvailabilityStatus.ERROR.label)
    }

    // 3. AI Capability Representation
    @Test
    fun testAICapabilityRepresentation() {
        val caps = AICapability.values()
        assertTrue(caps.contains(AICapability.TEXT_GENERATION))
        assertTrue(caps.contains(AICapability.CHAT))
        assertTrue(caps.contains(AICapability.STRUCTURED_OUTPUT))
        assertTrue(caps.contains(AICapability.IMAGE_UNDERSTANDING))
        assertTrue(caps.contains(AICapability.SPEECH_RECOGNITION))
        assertTrue(caps.contains(AICapability.SUMMARIZATION))
    }

    // 4. AI Model Metadata
    @Test
    fun testAIModelMetadata() {
        val metadata = AIModelMetadata(
            provider = AIProviderType.ON_DEVICE_GENAI,
            modelIdentifier = "gemini-nano-test",
            displayName = "Gemini Nano Test",
            modelVersion = "1.0",
            availabilityStatus = AIAvailabilityStatus.AVAILABLE,
            supportedCapabilities = setOf(AICapability.CHAT, AICapability.TEXT_GENERATION),
            offlineCapable = true,
            multimodalSupport = false
        )

        assertEquals("gemini-nano-test", metadata.modelIdentifier)
        assertEquals("Gemini Nano Test", metadata.displayName)
        assertTrue(metadata.offlineCapable)
        assertFalse(metadata.multimodalSupport)
        assertTrue(metadata.supportedCapabilities.contains(AICapability.CHAT))
    }

    // 5. Device AI Profile Creation
    @Test
    fun testDeviceAIProfile() {
        val profile = DeviceAIProfile(
            apiLevel = 36,
            manufacturer = "Samsung",
            model = "SM-S928B",
            totalMemoryBytes = 12_000_000_000L,
            availableMemoryBytes = 6_000_000_000L,
            totalStorageBytes = 256_000_000_000L,
            availableStorageBytes = 120_000_000_000L,
            isNetworkAvailable = false,
            onDeviceGenAIAvailability = AIAvailabilityStatus.AVAILABLE,
            recommendedProvider = AIProviderType.ON_DEVICE_GENAI
        )

        assertEquals(36, profile.apiLevel)
        assertEquals("Samsung", profile.manufacturer)
        assertEquals("SM-S928B", profile.model)
        assertFalse(profile.isNetworkAvailable)
        assertEquals(AIAvailabilityStatus.AVAILABLE, profile.onDeviceGenAIAvailability)
        assertEquals(AIProviderType.ON_DEVICE_GENAI, profile.recommendedProvider)
    }

    // 6. Registry Registration and Lookup
    @Test
    fun testRegistryRegistrationAndLookup() {
        val registry = AIModelRegistry()
        val dummyEngine = OfflineLocalAIEngine()
        val meta = AIModelMetadata(
            provider = AIProviderType.DETERMINISTIC_LOCAL,
            modelIdentifier = "offline-core",
            displayName = "Offline Core",
            availabilityStatus = AIAvailabilityStatus.AVAILABLE,
            supportedCapabilities = setOf(AICapability.CHAT, AICapability.TEXT_GENERATION)
        )

        registry.register(AIProviderType.DETERMINISTIC_LOCAL, dummyEngine, meta)

        val retrievedEngine = registry.getEngine(AIProviderType.DETERMINISTIC_LOCAL)
        val retrievedMeta = registry.getMetadata(AIProviderType.DETERMINISTIC_LOCAL)

        assertNotNull(retrievedEngine)
        assertNotNull(retrievedMeta)
        assertEquals("Offline Core", retrievedMeta?.displayName)
        assertEquals(AIAvailabilityStatus.AVAILABLE, retrievedMeta?.availabilityStatus)

        val available = registry.getAvailableModels()
        assertEquals(1, available.size)

        val chatProviders = registry.getProvidersForCapability(AICapability.CHAT)
        assertTrue(chatProviders.contains(AIProviderType.DETERMINISTIC_LOCAL))
    }

    // 7. Router selects available On-Device Provider when Ready
    @Test
    fun testRouterSelectsAvailableOnDeviceProvider() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val onDevice = OnDeviceGenAIProvider(initialStatus = AIAvailabilityStatus.AVAILABLE)

        registry.register(
            AIProviderType.ON_DEVICE_GENAI,
            onDevice,
            onDevice.metadata.copy(availabilityStatus = AIAvailabilityStatus.AVAILABLE)
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback
        )

        val (providerType, engine) = router.route(AICapability.TEXT_GENERATION)
        assertEquals(AIProviderType.ON_DEVICE_GENAI, providerType)
        assertEquals(onDevice, engine)
    }

    // 8. Router falls back to Deterministic Local when On-Device is Unavailable
    @Test
    fun testRouterFallsBackWhenOnDeviceUnavailable() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val onDevice = OnDeviceGenAIProvider(initialStatus = AIAvailabilityStatus.UNAVAILABLE)

        registry.register(
            AIProviderType.ON_DEVICE_GENAI,
            onDevice,
            onDevice.metadata.copy(availabilityStatus = AIAvailabilityStatus.UNAVAILABLE)
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback
        )

        val (providerType, engine) = router.route(AICapability.TEXT_GENERATION)
        assertEquals(AIProviderType.DETERMINISTIC_LOCAL, providerType)
        assertEquals(fallback, engine)
    }

    // 9. Router does NOT select Cloud in Phase 0C
    @Test
    fun testRouterDoesNotSelectCloud() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val dummyCloudEngine = object : AIEngine {
            override val modelInfo = AIModelInfo("Cloud", "1.0", false, "Cloud model")
            override val isReady: Boolean = true
            override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult = throw UnsupportedOperationException()
            override suspend fun classifyIntent(input: String): SummerIntent = SummerIntent.Unknown(input)
            override suspend fun processQuery(request: AIRequest): AIResponse = throw UnsupportedOperationException()
            override suspend fun evaluateIntent(input: String): RecognizedIntent = throw UnsupportedOperationException()
        }

        registry.register(
            AIProviderType.CLOUD,
            dummyCloudEngine,
            AIModelMetadata(
                provider = AIProviderType.CLOUD,
                modelIdentifier = "cloud-llm",
                displayName = "Cloud LLM",
                availabilityStatus = AIAvailabilityStatus.AVAILABLE,
                supportedCapabilities = setOf(AICapability.TEXT_GENERATION),
                offlineCapable = false
            )
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback
        )

        val (providerType, _) = router.route(AICapability.TEXT_GENERATION)
        // Must choose deterministic local, NOT cloud
        assertEquals(AIProviderType.DETERMINISTIC_LOCAL, providerType)
    }

    // 10. Capability detection failure does NOT crash Summer
    @Test
    fun testCapabilityDetectionFailureHandledGracefully() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val faultyDetector = FakeCapabilityDetector(shouldThrow = true)

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            detector = faultyDetector
        )

        val profile = router.refreshCapabilities()
        assertNull(profile)

        val diagnostics = router.diagnostics.value
        assertEquals(AIAvailabilityStatus.ERROR, diagnostics.providerAvailability)
        assertTrue(diagnostics.isFallbackActive)
        assertEquals(AIProviderType.DETERMINISTIC_LOCAL, diagnostics.currentProvider)

        // Router still works via fallback
        val (providerType, engine) = router.route()
        assertEquals(AIProviderType.DETERMINISTIC_LOCAL, providerType)
        assertEquals(fallback, engine)
    }

    // 11. Offline state does NOT falsely indicate AI is unavailable
    @Test
    fun testOfflineStateDoesNotEqualAIFailure() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val networkProvider = FakeNetworkProvider(initialState = NetworkState.OFFLINE)
        val detector = FakeCapabilityDetector(
            profileToReturn = DeviceAIProfile(
                apiLevel = 36,
                manufacturer = "Google",
                model = "Pixel 9 Pro",
                isNetworkAvailable = false,
                onDeviceGenAIAvailability = AIAvailabilityStatus.AVAILABLE,
                recommendedProvider = AIProviderType.ON_DEVICE_GENAI
            )
        )

        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback,
            detector = detector,
            networkProvider = networkProvider
        )

        val onDevice = OnDeviceGenAIProvider(initialStatus = AIAvailabilityStatus.AVAILABLE)
        registry.register(AIProviderType.ON_DEVICE_GENAI, onDevice, onDevice.metadata.copy(availabilityStatus = AIAvailabilityStatus.AVAILABLE))

        router.refreshCapabilities()

        val diagnostics = router.diagnostics.value
        assertFalse(diagnostics.isNetworkAvailable)
        assertEquals(AIAvailabilityStatus.AVAILABLE, diagnostics.providerAvailability)
        assertEquals(AIProviderType.ON_DEVICE_GENAI, diagnostics.currentProvider)
    }

    // 12. Deterministic demonstration intents continue working through router
    @Test
    fun testPhase0BIntentsThroughRouter() = runTest {
        val registry = AIModelRegistry()
        val fallback = OfflineLocalAIEngine()
        val router = AIModelRouter(
            registry = registry,
            fallbackEngine = fallback
        )

        val context = SummerContext(sessionId = "test-session")
        val greetingInteraction = SummerInteraction(sessionId = "test-session", userInput = "Hello Summer")
        val greetingResult = router.process(context, greetingInteraction)
        assertEquals("Hello. I'm Summer. How can I assist you?", greetingResult.response.text)

        val nameInteraction = SummerInteraction(sessionId = "test-session", userInput = "What is your name?")
        val nameResult = router.process(context, nameInteraction)
        assertEquals("I'm Summer Winter. You can call me Summer.", nameResult.response.text)

        val capInteraction = SummerInteraction(sessionId = "test-session", userInput = "What can you do?")
        val capResult = router.process(context, capInteraction)
        assertTrue(capResult.response.text.contains("architecture is being prepared"))
    }

    // 13. Router strictly falls back on every non-AVAILABLE status
    @Test
    fun testRouterFallsBackOnAllNonAvailableStates() = runTest {
        val nonAvailableStatuses = listOf(
            AIAvailabilityStatus.CHECKING,
            AIAvailabilityStatus.DOWNLOADABLE,
            AIAvailabilityStatus.DOWNLOADING,
            AIAvailabilityStatus.UNAVAILABLE,
            AIAvailabilityStatus.NOT_SUPPORTED,
            AIAvailabilityStatus.ERROR
        )

        for (status in nonAvailableStatuses) {
            val registry = AIModelRegistry()
            val fallback = OfflineLocalAIEngine()
            val onDevice = OnDeviceGenAIProvider(initialStatus = status)

            registry.register(
                AIProviderType.ON_DEVICE_GENAI,
                onDevice,
                onDevice.metadata.copy(availabilityStatus = status)
            )

            val router = AIModelRouter(
                registry = registry,
                fallbackEngine = fallback
            )

            val (routedType, routedEngine) = router.route(AICapability.TEXT_GENERATION)
            assertEquals("Status $status must route to DETERMINISTIC_LOCAL", AIProviderType.DETERMINISTIC_LOCAL, routedType)
            assertEquals(fallback, routedEngine)
        }
    }
}
