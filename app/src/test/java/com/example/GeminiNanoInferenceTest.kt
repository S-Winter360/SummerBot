package com.example

import com.example.actions.ActionRequest
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.capability.AIAvailabilityStatus
import com.example.ai.capability.GeminiClient
import com.example.ai.capability.GeminiResponseParser
import com.example.ai.capability.OnDeviceGeminiNanoAIEngine
import com.example.ai.capability.SummerPromptBuilder
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.personality.SummerPersonality
import com.example.core.response.ResponseType
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.network.NetworkState
import com.example.security.Capability
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for Phase 0D: Real Local Generative AI Inference with ML Kit GenAI Prompt API / Gemini Nano.
 */
class GeminiNanoInferenceTest {

    private class FakeGeminiClient(
        var statusToReturn: AIAvailabilityStatus = AIAvailabilityStatus.AVAILABLE,
        var textToReturn: String? = "Hello. I am Summer. How can I help you today?",
        var shouldThrow: Boolean = false
    ) : GeminiClient {
        var lastPromptReceived: String? = null
        var warmupCalled = false

        override suspend fun checkStatus(): AIAvailabilityStatus {
            return statusToReturn
        }

        override suspend fun generateContent(prompt: String): String? {
            lastPromptReceived = prompt
            if (shouldThrow) {
                throw IllegalStateException("Simulated on-device model generation fault or busy quota")
            }
            return textToReturn
        }

        override suspend fun warmup() {
            warmupCalled = true
        }
    }

    @Test
    fun testPromptBuilderConstructsStructuredContext() {
        val builder = SummerPromptBuilder(
            personality = SummerPersonality.DEFAULT,
            maxRecentInteractions = 3,
            maxMemories = 3
        )

        val pastInteraction = SummerInteraction(
            sessionId = "session-1",
            userInput = "Hi there",
            response = com.example.core.response.SummerResponse(text = "Hello! Ready.")
        )

        val memory = MemoryRecord(
            category = MemoryCategory.USER_PREFERENCE,
            title = "Favorite Theme",
            content = "User prefers cyan bioluminescent theme."
        )

        val context = SummerContext(
            sessionId = "session-1",
            recentInteractions = listOf(pastInteraction),
            activeMemories = listOf(memory),
            networkState = NetworkState.CONNECTED_WIFI,
            timestamp = 1700000000000L
        )

        val currentInteraction = SummerInteraction(
            sessionId = "session-1",
            userInput = "What is your purpose?"
        )

        val prompt = builder.buildPrompt(context, currentInteraction)

        assertTrue(prompt.contains("Summer Winter"))
        assertTrue(prompt.contains("observant"))
        assertTrue(prompt.contains("Known Explicit User Memories"))
        assertTrue(prompt.contains("User prefers cyan bioluminescent theme"))
        assertTrue(prompt.contains("Recent Conversation"))
        assertTrue(prompt.contains("Hi there"))
        assertTrue(prompt.contains("Hello! Ready."))
        assertTrue(prompt.contains("Current User Message"))
        assertTrue(prompt.contains("What is your purpose?"))
    }

    @Test
    fun testResponseParserClassifiesGreeting() {
        val parser = GeminiResponseParser()
        val interaction = SummerInteraction(sessionId = "s1", userInput = "Good morning Summer!")
        val result = parser.parse("Good morning! It is peaceful today.", interaction)

        assertTrue(result.intent is SummerIntent.Greeting)
        assertEquals("Good morning! It is peaceful today.", result.response.text)
        assertEquals(0.95f, result.confidence, 0.01f)
    }

    @Test
    fun testResponseParserClassifiesIdentity() {
        val parser = GeminiResponseParser()
        val interaction = SummerInteraction(sessionId = "s1", userInput = "Tell me about yourself")
        val result = parser.parse("I'm Summer Winter, your local on-device companion.", interaction)

        assertTrue(result.intent is SummerIntent.IdentityQuestion)
        assertEquals(ResponseType.INFORMATION, result.response.type)
    }

    @Test
    fun testResponseParserActionProposalIsolation() {
        val parser = GeminiResponseParser()
        val interaction = SummerInteraction(sessionId = "s1", userInput = "Test network connection")
        val result = parser.parse("I can propose a connectivity check.", interaction)

        assertTrue(result.intent is SummerIntent.CapabilityAction)
        assertEquals(1, result.actionRequests.size)
        assertEquals(Capability.INTERNET, result.actionRequests.first().capability)
        assertEquals(ResponseType.ACTION_PROPOSAL, result.response.type)
    }

    @Test
    fun testResponseParserMemoryBoundaryExplicitOnly() {
        val parser = GeminiResponseParser()

        // Non-explicit statement: should NOT create memory
        val normalInteraction = SummerInteraction(sessionId = "s1", userInput = "My favorite color is green.")
        val normalResult = parser.parse("Green is very calming.", normalInteraction)
        assertTrue(normalResult.memorySuggestions.isEmpty())

        // Explicit "remember" command: SHOULD create memory
        val explicitInteraction = SummerInteraction(sessionId = "s1", userInput = "Remember that I like Earl Grey tea.")
        val explicitResult = parser.parse("I have recorded that you like Earl Grey tea.", explicitInteraction)
        assertEquals(1, explicitResult.memorySuggestions.size)
        assertEquals("I like Earl Grey tea.", explicitResult.memorySuggestions.first().content)
        assertEquals(MemoryCategory.USER_PREFERENCE, explicitResult.memorySuggestions.first().category)
    }

    @Test
    fun testResponseParserHandlesMalformedAndEmptyOutput() {
        val parser = GeminiResponseParser()
        val interaction = SummerInteraction(sessionId = "s1", userInput = "Hello")

        val emptyResult = parser.parse("", interaction)
        assertFalse(emptyResult.response.text.isBlank())

        val prefixResult = parser.parse("Summer: Hello! How can I assist?", interaction)
        assertEquals("Hello! How can I assist?", prefixResult.response.text)
    }

    @Test
    fun testOnDeviceGeminiNanoEngineGeneratesResponseWhenAvailable() = runTest {
        val fakeClient = FakeGeminiClient(
            statusToReturn = AIAvailabilityStatus.AVAILABLE,
            textToReturn = "Summer here. The local morning air is calm."
        )

        val engine = OnDeviceGeminiNanoAIEngine(
            client = fakeClient,
            initialStatus = AIAvailabilityStatus.AVAILABLE
        )

        assertTrue(engine.isReady)

        val context = SummerContext(sessionId = "s1")
        val interaction = SummerInteraction(sessionId = "s1", userInput = "How are things?")
        val result = engine.process(context, interaction)

        assertEquals("Summer here. The local morning air is calm.", result.response.text)
        assertEquals("Gemini Nano (ML Kit Prompt API)", result.response.source)
        assertNotNull(fakeClient.lastPromptReceived)
    }

    @Test
    fun testOnDeviceGeminiNanoEngineFallsBackWhenClientErrors() = runTest {
        val fallback = OfflineLocalAIEngine()
        val fakeClient = FakeGeminiClient(
            statusToReturn = AIAvailabilityStatus.AVAILABLE,
            shouldThrow = true
        )

        val engine = OnDeviceGeminiNanoAIEngine(
            client = fakeClient,
            fallbackEngine = fallback,
            initialStatus = AIAvailabilityStatus.AVAILABLE
        )

        val context = SummerContext(sessionId = "s1")
        val interaction = SummerInteraction(sessionId = "s1", userInput = "Hello Summer")
        val result = engine.process(context, interaction)

        // Must safely return fallback without crashing
        assertNotNull(result)
        assertTrue(result.response.text.contains("Summer"))
    }

    @Test
    fun testOnDeviceGeminiNanoEngineFallsBackWhenNotAvailable() = runTest {
        val fallback = OfflineLocalAIEngine()
        val fakeClient = FakeGeminiClient(
            statusToReturn = AIAvailabilityStatus.NOT_SUPPORTED
        )

        val engine = OnDeviceGeminiNanoAIEngine(
            client = fakeClient,
            fallbackEngine = fallback,
            initialStatus = AIAvailabilityStatus.NOT_SUPPORTED
        )

        assertFalse(engine.isReady)

        val context = SummerContext(sessionId = "s1")
        val interaction = SummerInteraction(sessionId = "s1", userInput = "Who are you?")
        val result = engine.process(context, interaction)

        // Must fall back to deterministic response
        assertEquals("I'm Summer Winter. You can call me Summer.", result.response.text)
    }

    @Test
    fun testWarmupExecutesSafely() = runTest {
        val fakeClient = FakeGeminiClient(statusToReturn = AIAvailabilityStatus.AVAILABLE)
        val engine = OnDeviceGeminiNanoAIEngine(
            client = fakeClient,
            initialStatus = AIAvailabilityStatus.AVAILABLE
        )

        engine.warmup()
        assertTrue(fakeClient.warmupCalled)
    }
}
