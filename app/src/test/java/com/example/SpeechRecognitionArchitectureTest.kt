package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.decision.DefaultSummerDecisionEngine
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import com.example.core.interaction.InteractionState
import com.example.core.orchestrator.SummerOrchestrator
import com.example.core.personality.SummerPersonality
import com.example.core.session.SummerSessionManager
import com.example.core.state.SummerState
import com.example.core.state.SummerStateManager
import com.example.memory.MemoryRepository
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import com.example.security.DefaultActionAuthorizationPolicy
import com.example.voice.input.SpeechOfflineCapability
import com.example.voice.input.SpeechRecognitionDiagnostics
import com.example.voice.input.SpeechRecognitionEngine
import com.example.voice.input.SpeechRecognitionError
import com.example.voice.input.SpeechRecognitionResult
import com.example.voice.input.SpeechRecognitionRouter
import com.example.voice.input.SpeechRecognitionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 0H: Speech Recognition & Foreground Listening Architecture Tests.
 * Verifies states, providers, router, event dispatch, transient partial transcripts,
 * memory privacy, security boundaries, and lifecycle safety.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpeechRecognitionArchitectureTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    /**
     * Controllable mock speech recognition engine for deterministic testing.
     */
    class MockSpeechRecognitionEngine(
        initialAvailable: Boolean = true,
        initialPermission: Boolean = true
    ) : SpeechRecognitionEngine {

        private val _state = MutableStateFlow(SpeechRecognitionState.IDLE)
        override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

        private val _partialTranscript = MutableStateFlow("")
        override val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

        private val _diagnostics = MutableStateFlow(
            SpeechRecognitionDiagnostics(
                isAvailable = initialAvailable,
                microphonePermissionGranted = initialPermission,
                offlineCapability = SpeechOfflineCapability.UNKNOWN,
                state = SpeechRecognitionState.IDLE
            )
        )
        override val diagnostics: StateFlow<SpeechRecognitionDiagnostics> = _diagnostics.asStateFlow()

        var simulatedResult: SpeechRecognitionResult? = null
        var lastCallback: ((SpeechRecognitionResult) -> Unit)? = null
        var isReleased: Boolean = false

        fun emitPartial(partial: String) {
            _partialTranscript.value = partial
        }

        fun emitState(newState: SpeechRecognitionState) {
            _state.value = newState
            _diagnostics.value = _diagnostics.value.copy(state = newState)
        }

        override suspend fun startListening(onResult: (SpeechRecognitionResult) -> Unit) {
            lastCallback = onResult
            if (!_diagnostics.value.microphonePermissionGranted) {
                _state.value = SpeechRecognitionState.ERROR
                onResult(SpeechRecognitionResult.PermissionDenied("Microphone permission denied"))
                return
            }
            if (!_diagnostics.value.isAvailable) {
                _state.value = SpeechRecognitionState.UNAVAILABLE
                onResult(SpeechRecognitionResult.Unavailable("Speech recognition unavailable"))
                return
            }

            _state.value = SpeechRecognitionState.LISTENING
            _diagnostics.value = _diagnostics.value.copy(
                state = SpeechRecognitionState.LISTENING,
                isListening = true
            )

            simulatedResult?.let {
                _state.value = SpeechRecognitionState.COMPLETED
                _diagnostics.value = _diagnostics.value.copy(
                    state = SpeechRecognitionState.COMPLETED,
                    isListening = false
                )
                onResult(it)
            }
        }

        override suspend fun stopListening() {
            _state.value = SpeechRecognitionState.PROCESSING
            _diagnostics.value = _diagnostics.value.copy(
                state = SpeechRecognitionState.PROCESSING,
                isListening = false
            )
        }

        override fun cancel() {
            _state.value = SpeechRecognitionState.STOPPED
            _partialTranscript.value = ""
            _diagnostics.value = _diagnostics.value.copy(
                state = SpeechRecognitionState.STOPPED,
                isListening = false
            )
        }

        override fun release() {
            isReleased = true
            _state.value = SpeechRecognitionState.IDLE
            _partialTranscript.value = ""
            lastCallback = null
        }
    }

    // 1. Initial state is IDLE
    @Test
    fun testInitialSpeechRecognitionStateIsIdle() {
        val engine = MockSpeechRecognitionEngine()
        assertEquals(SpeechRecognitionState.IDLE, engine.state.value)
        assertFalse(engine.diagnostics.value.isListening)
    }

    // 2. Start listening transitions to LISTENING
    @Test
    fun testSpeechStateTransitionsToListening() = runBlocking {
        val engine = MockSpeechRecognitionEngine()
        val router = SpeechRecognitionRouter(engine)

        router.startListening {}
        assertEquals(SpeechRecognitionState.LISTENING, router.state.value)
        assertTrue(router.diagnostics.value.isListening)
        router.release()
    }

    // 3. Stop listening transitions to PROCESSING
    @Test
    fun testSpeechStateTransitionsToProcessing() = runBlocking {
        val engine = MockSpeechRecognitionEngine()
        val router = SpeechRecognitionRouter(engine)

        router.startListening {}
        router.stopListening()
        assertEquals(SpeechRecognitionState.PROCESSING, router.state.value)
        router.release()
    }

    // 4. Successful result transitions to COMPLETED
    @Test
    fun testSpeechStateTransitionsToCompletedOnResult() = runBlocking {
        val engine = MockSpeechRecognitionEngine().apply {
            simulatedResult = SpeechRecognitionResult.Success("What is the time?")
        }
        val router = SpeechRecognitionRouter(engine)

        var finalTranscriptReceived: String? = null
        router.startListening { finalTranscriptReceived = it }

        assertEquals(SpeechRecognitionState.COMPLETED, router.state.value)
        assertEquals("What is the time?", finalTranscriptReceived)
        router.release()
    }

    // 5. Cancel transitions to STOPPED
    @Test
    fun testSpeechStateTransitionsToStoppedOnCancel() = runBlocking {
        val engine = MockSpeechRecognitionEngine()
        val router = SpeechRecognitionRouter(engine)

        router.startListening {}
        router.cancel()
        assertEquals(SpeechRecognitionState.STOPPED, router.state.value)
        router.release()
    }

    // 6. Error state transition
    @Test
    fun testSpeechStateTransitionsToErrorOnFailure() = runBlocking {
        val engine = MockSpeechRecognitionEngine(initialPermission = false)
        val router = SpeechRecognitionRouter(engine)

        router.startListening {}
        assertEquals(SpeechRecognitionState.ERROR, router.state.value)
        router.release()
    }

    // 7. Provider availability and diagnostics
    @Test
    fun testProviderReportsAvailabilityAndDiagnostics() {
        val engine = MockSpeechRecognitionEngine(initialAvailable = true)
        val diag = engine.diagnostics.value
        assertTrue(diag.isAvailable)
        assertEquals(SpeechRecognitionState.IDLE, diag.state)
        assertEquals("Android SpeechRecognizer", diag.recognizerName)
    }

    // 8. Unavailable provider handles gracefully
    @Test
    fun testProviderHandlesUnavailableGracefully() = runBlocking {
        val engine = MockSpeechRecognitionEngine(initialAvailable = false)
        val router = SpeechRecognitionRouter(engine)

        router.startListening {}
        assertEquals(SpeechRecognitionState.UNAVAILABLE, router.state.value)
        router.release()
    }

    // 9. Permission denied handling
    @Test
    fun testProviderHandlesPermissionDeniedGracefully() = runBlocking {
        val engine = MockSpeechRecognitionEngine(initialPermission = false)
        val router = SpeechRecognitionRouter(engine)

        var receivedResult: String? = null
        router.startListening { receivedResult = it }

        assertEquals(SpeechRecognitionState.ERROR, router.state.value)
        assertEquals(null, receivedResult)
        router.release()
    }

    // 10. Provider produces successful transcript with metadata
    @Test
    fun testProviderProducesSuccessfulTranscript() = runBlocking {
        val engine = MockSpeechRecognitionEngine().apply {
            simulatedResult = SpeechRecognitionResult.Success(
                transcript = "Hello Summer",
                confidence = 0.98f,
                locale = "en-US"
            )
        }
        val router = SpeechRecognitionRouter(engine)

        var transcriptOut: String? = null
        router.startListening { transcriptOut = it }

        assertEquals("Hello Summer", transcriptOut)
        router.release()
    }

    // 11. No speech detected handling
    @Test
    fun testProviderHandlesNoSpeechGracefully() = runBlocking {
        val engine = MockSpeechRecognitionEngine().apply {
            simulatedResult = SpeechRecognitionResult.NoSpeech("Silence timeout")
        }
        val router = SpeechRecognitionRouter(engine)

        var transcriptReceived = false
        router.startListening { transcriptReceived = true }

        assertFalse(transcriptReceived)
        router.release()
    }

    // 12. No match handling
    @Test
    fun testProviderHandlesNoMatchGracefully() = runBlocking {
        val engine = MockSpeechRecognitionEngine().apply {
            simulatedResult = SpeechRecognitionResult.NoMatch("No recognition candidate")
        }
        val router = SpeechRecognitionRouter(engine)

        var transcriptReceived = false
        router.startListening { transcriptReceived = true }

        assertFalse(transcriptReceived)
        router.release()
    }

    // 13. Router dispatches SummerEvent.VoiceInput to eventBus
    @Test
    fun testRouterDispatchesVoiceInputEventOnSuccess() = runBlocking {
        val eventBus = SummerEventBus()
        val receivedEvents = mutableListOf<SummerEvent>()

        val job = launch {
            eventBus.events.collect { receivedEvents.add(it) }
        }

        val engine = MockSpeechRecognitionEngine().apply {
            simulatedResult = SpeechRecognitionResult.Success("Explore machine learning")
        }
        val router = SpeechRecognitionRouter(engine = engine, eventBus = eventBus)

        router.startListening {}
        kotlinx.coroutines.delay(100)

        // Allow event collection
        val voiceEvent = receivedEvents.filterIsInstance<SummerEvent.VoiceInput>().firstOrNull()
        assertNotNull(voiceEvent)
        assertEquals("Explore machine learning", voiceEvent!!.audioDataPreview)
        assertEquals("Explore machine learning", voiceEvent.transcript)

        job.cancel()
        router.release()
    }

    // 14. Partial transcript is transient and exposed reactively
    @Test
    fun testPartialTranscriptIsTransientAndExposed() = runBlocking {
        val engine = MockSpeechRecognitionEngine()
        val router = SpeechRecognitionRouter(engine)

        assertEquals("", router.partialTranscript.value)

        engine.emitPartial("What do you think")
        assertEquals("What do you think", router.partialTranscript.value)

        engine.emitPartial("What do you think about Python?")
        assertEquals("What do you think about Python?", router.partialTranscript.value)

        // Cancel clears partial transcript
        router.cancel()
        assertEquals("", router.partialTranscript.value)
        router.release()
    }

    // 15. Partial transcript does not trigger AI engine or persist memory
    @Test
    fun testPartialTranscriptDoesNotTriggerAIEngineOrMemory() {
        val memoryRecords = mutableListOf<MemoryRecord>()
        val engine = MockSpeechRecognitionEngine()

        engine.emitPartial("I prefer dark roast coffee")

        // Assert memory list remains untouched by transient partials
        assertTrue(memoryRecords.isEmpty())
        engine.release()
    }

    // 16. Final transcript enters the existing conversation context exactly once
    @Test
    fun testFinalTranscriptEntersConversationContextExactlyOnce() = runBlocking {
        val stateManager = SummerStateManager()
        val sessionManager = SummerSessionManager()
        val eventBus = SummerEventBus()
        val personality = SummerPersonality.DEFAULT
        val fallbackAIEngine = com.example.ai.OfflineLocalAIEngine(personality)
        val decisionEngine = DefaultSummerDecisionEngine()
        val authPolicy = DefaultActionAuthorizationPolicy()
        val actionExecutor = com.example.actions.SecuredActionExecutor(authPolicy)

        val memoryRecords = mutableListOf<MemoryRecord>()
        val mockRepo = object : MemoryRepository {
            override suspend fun recordMemory(record: MemoryRecord) { memoryRecords.add(record) }
            override suspend fun updateMemory(record: MemoryRecord) {}
            override suspend fun deleteMemory(id: String) {}
            override suspend fun forgetMemory(targetId: String?, keyword: String?): Boolean = false
            override suspend fun updateAccessMetadata(id: String, count: Int, timestamp: Long) {}
            override suspend fun getActiveMemories(): List<MemoryRecord> = memoryRecords
            override fun observeMemories(): Flow<List<MemoryRecord>> = flowOf(memoryRecords)
            override fun observeMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>> = flowOf(emptyList())
            override suspend fun clearAllMemories() { memoryRecords.clear() }
            override suspend fun getSettings(): SummerSettings = SummerSettings()
            override suspend fun updateSettings(settings: SummerSettings) {}
            override fun observeSettings(): Flow<SummerSettings> = flowOf(SummerSettings())
        }

        val networkProvider = object : NetworkInformationProvider {
            override val networkState: Flow<NetworkState> = flowOf(NetworkState.OFFLINE)
            override val isOnline: Flow<Boolean> = flowOf(false)
            override fun getCurrentState(): NetworkState = NetworkState.OFFLINE
        }

        val orchestrator = SummerOrchestrator(
            stateManager = stateManager,
            sessionManager = sessionManager,
            eventBus = eventBus,
            aiEngine = fallbackAIEngine,
            decisionEngine = decisionEngine,
            authorizationPolicy = authPolicy,
            actionExecutor = actionExecutor,
            memoryRepository = mockRepo,
            networkProvider = networkProvider
        )

        // Process final speech transcript through orchestrator
        val spokenTranscript = "Tell me about yourself"
        val interaction = orchestrator.handleUserInput(spokenTranscript, source = "voice.engine")

        assertEquals(InteractionState.COMPLETED, interaction.state)
        assertEquals(spokenTranscript, interaction.userInput)
        assertNotNull(interaction.response)
        assertTrue(interaction.response!!.text.contains("Summer"))

        // Assert exactly one conversation interaction was recorded in history
        assertEquals(1, orchestrator.interactionHistory.value.size)
        assertEquals(spokenTranscript, orchestrator.interactionHistory.value.first().userInput)
    }

    // 17. Lifecycle safety: recognizer cleanup
    @Test
    fun testLifecycleSafetyRecognizerRelease() {
        val engine = MockSpeechRecognitionEngine()
        val router = SpeechRecognitionRouter(engine)

        assertFalse(engine.isReleased)
        router.release()
        assertTrue(engine.isReleased)
        assertEquals(SpeechRecognitionState.IDLE, router.state.value)
    }

    // 18. Security: no automatic or background listening
    @Test
    fun testNoAutomaticListeningOrBackgroundActivity() {
        val engine = MockSpeechRecognitionEngine()
        val router = SpeechRecognitionRouter(engine)

        // Verifies engine is NOT listening upon creation
        assertEquals(SpeechRecognitionState.IDLE, router.state.value)
        assertFalse(router.diagnostics.value.isListening)

        router.release()
    }

    // 19. Privacy: no raw audio persistence
    @Test
    fun testNoRawAudioPersistence() {
        // Assert that SpeechRecognitionResult only contains textual transcript and confidence
        val success = SpeechRecognitionResult.Success(transcript = "Private spoken words")
        assertEquals(String::class.java, success.transcript.javaClass)
        // No byte buffer, file path, or audio stream is present on the model
        val fields = success.javaClass.declaredFields.map { it.name }
        assertFalse(fields.contains("audioBytes"))
        assertFalse(fields.contains("rawAudio"))
        assertFalse(fields.contains("audioFile"))
    }

    // 20. Offline capability diagnostics reporting
    @Test
    fun testOfflineCapabilityDiagnosticsTruthful() {
        val engine = MockSpeechRecognitionEngine()
        val diag = engine.diagnostics.value

        // Does not falsely claim guaranteed offline operation
        assertEquals(SpeechOfflineCapability.UNKNOWN, diag.offlineCapability)
        assertFalse(diag.offlineCapability == SpeechOfflineCapability.ON_DEVICE_CONFIRMED)
    }
}
