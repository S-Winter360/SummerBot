package com.example

import com.example.voice.engine.ResolvedVoice
import com.example.voice.engine.SpeechItem
import com.example.voice.engine.SpeechQueue
import com.example.voice.engine.SpeechSegmenter
import com.example.voice.engine.VoiceEngineRouter
import com.example.voice.engine.VoiceProfileResolver
import com.example.voice.models.SpeechState
import com.example.voice.models.VoiceCharacteristics
import com.example.voice.models.VoiceGender
import com.example.voice.models.VoiceProfile
import com.example.voice.models.VoiceProfileId
import com.example.voice.models.VoiceProviderType
import com.example.voice.provider.NaturalVoiceProvider
import com.example.voice.provider.VoiceProvider
import com.example.memory.models.SummerSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Comprehensive unit test suite for Phase 0F: Voice Architecture & Natural Speech Foundation.
 */
class VoiceArchitectureTest {

    private class FakeVoiceProvider(
        override val providerType: VoiceProviderType = VoiceProviderType.SYSTEM_OFFLINE,
        override var isAvailable: Boolean = true,
        var engineVoices: List<String> = listOf("en-us-x-sfg#female_2", "en-us-x-tpd#male_1")
    ) : VoiceProvider {
        override var isInitialized: Boolean = true
        var speakCallCount = 0
        var stopCallCount = 0
        var lastSpokenText: String? = null
        var lastProfile: VoiceProfile? = null

        override suspend fun initialize(): Boolean = isAvailable

        override suspend fun speak(
            text: String,
            profile: VoiceProfile,
            onStarted: () -> Unit,
            onDone: () -> Unit,
            onError: (String) -> Unit
        ): Boolean {
            speakCallCount++
            lastSpokenText = text
            lastProfile = profile
            onStarted()
            onDone()
            return true
        }

        override suspend fun stop() {
            stopCallCount++
        }

        override fun getAvailableEngineVoices(): List<String> = engineVoices
        override fun release() {}
    }

    private class FakeNaturalVoiceProvider(
        override var isAvailable: Boolean = false
    ) : NaturalVoiceProvider {
        override val providerType: VoiceProviderType = VoiceProviderType.NATURAL_LOCAL
        override var isInitialized: Boolean = true
        var speakCallCount = 0

        override suspend fun initialize(): Boolean = isAvailable

        override suspend fun speak(
            text: String,
            profile: VoiceProfile,
            onStarted: () -> Unit,
            onDone: () -> Unit,
            onError: (String) -> Unit
        ): Boolean {
            speakCallCount++
            onStarted()
            onDone()
            return true
        }

        override suspend fun stop() {}
        override fun release() {}
    }

    // 1. VoiceProfile creation & defaults
    @Test
    fun testVoiceProfileCreationAndDefaults() {
        val profile = VoiceProfile(
            id = VoiceProfileId.FEMALE,
            displayName = "Summer Female",
            gender = VoiceGender.FEMALE,
            speechRate = 1.0f,
            pitch = 1.0f
        )

        assertEquals(VoiceProfileId.FEMALE, profile.id)
        assertEquals("Summer Female", profile.displayName)
        assertEquals(VoiceGender.FEMALE, profile.gender)
        assertEquals(1.0f, profile.speechRate, 0.01f)
        assertEquals(1.0f, profile.pitch, 0.01f)
        assertTrue(profile.isAvailable)
        assertTrue(profile.isBuiltIn)
    }

    // 2. Female profile target characteristics
    @Test
    fun testFemaleProfileCreation() {
        val female = VoiceProfile.SUMMER_FEMALE
        assertEquals(VoiceProfileId.FEMALE, female.id)
        assertEquals(VoiceGender.FEMALE, female.gender)
        assertTrue(female.displayName.contains("Female"))
        assertTrue(female.targetCharacteristics.warmth >= 0.8f)
        assertTrue(female.targetCharacteristics.tone.contains("warm"))
        assertTrue(female.targetCharacteristics.tone.contains("conversational"))
    }

    // 3. Male profile target characteristics
    @Test
    fun testMaleProfileCreation() {
        val male = VoiceProfile.SUMMER_MALE
        assertEquals(VoiceProfileId.MALE, male.id)
        assertEquals(VoiceGender.MALE, male.gender)
        assertTrue(male.displayName.contains("Male"))
        assertTrue(male.targetCharacteristics.warmth >= 0.8f)
        assertTrue(male.targetCharacteristics.tone.contains("composed"))
        assertTrue(male.targetCharacteristics.tone.contains("intelligent"))
    }

    // 4. Default voice preference
    @Test
    fun testDefaultVoicePreference() {
        val settings = SummerSettings()
        assertEquals("FEMALE", settings.voiceProfileId)
        val defaultProfile = VoiceProfile.fromString(settings.voiceProfileId)
        assertEquals(VoiceProfileId.FEMALE, defaultProfile.id)
    }

    // 5. Voice preference persistence in SummerSettings
    @Test
    fun testVoicePreferencePersistenceModel() {
        val settings = SummerSettings(
            voiceProfileId = "MALE",
            speechSpeed = 1.25f,
            speechPitch = 0.95f,
            speechVolume = 0.90f
        )

        assertEquals("MALE", settings.voiceProfileId)
        assertEquals(1.25f, settings.speechSpeed, 0.01f)
        assertEquals(0.95f, settings.speechPitch, 0.01f)
        assertEquals(0.90f, settings.speechVolume, 0.01f)
    }

    // 6. Voice profile equality & stability
    @Test
    fun testVoiceProfileEqualityAndStability() {
        val p1 = VoiceProfile.fromId(VoiceProfileId.FEMALE)
        val p2 = VoiceProfile.fromString("FEMALE")
        assertEquals(p1.id, p2.id)
        assertEquals(p1.gender, p2.gender)
    }

    // 7. Voice profile resolver selects matching available voice
    @Test
    fun testVoiceProfileResolverSelectsMatchingVoice() {
        val resolver = VoiceProfileResolver()
        val availableVoices = listOf(
            "en-us-x-sfg#female_2",
            "en-us-x-tpd#male_1",
            "es-es-x-ana#female_1"
        )

        val resolvedFemale = resolver.resolve(VoiceProfile.SUMMER_FEMALE, availableVoices)
        assertTrue(resolvedFemale.isExactGenderMatch)
        assertFalse(resolvedFemale.isFallback)
        assertEquals("en-us-x-sfg#female_2", resolvedFemale.engineVoiceId)

        val resolvedMale = resolver.resolve(VoiceProfile.SUMMER_MALE, availableVoices)
        assertTrue(resolvedMale.isExactGenderMatch)
        assertFalse(resolvedMale.isFallback)
        assertEquals("en-us-x-tpd#male_1", resolvedMale.engineVoiceId)
    }

    // 8. Voice profile resolver handles missing female voice gracefully
    @Test
    fun testVoiceProfileResolverHandlesMissingFemaleVoice() {
        val resolver = VoiceProfileResolver()
        val availableVoices = listOf("en-us-x-tpd#male_1")

        val resolved = resolver.resolve(VoiceProfile.SUMMER_FEMALE, availableVoices)
        assertTrue(resolved.isFallback)
        assertNotNull(resolved.engineVoiceId)
        assertEquals("en-us-x-tpd#male_1", resolved.engineVoiceId)
    }

    // 9. Voice profile resolver handles missing male voice gracefully
    @Test
    fun testVoiceProfileResolverHandlesMissingMaleVoice() {
        val resolver = VoiceProfileResolver()
        val availableVoices = listOf("en-us-x-sfg#female_2")

        val resolved = resolver.resolve(VoiceProfile.SUMMER_MALE, availableVoices)
        assertTrue(resolved.isFallback)
        assertNotNull(resolved.engineVoiceId)
        assertEquals("en-us-x-sfg#female_2", resolved.engineVoiceId)
    }

    // 10. Voice resolver handles empty/no available voices safely
    @Test
    fun testVoiceResolverHandlesNoAvailableVoices() {
        val resolver = VoiceProfileResolver()
        val resolved = resolver.resolve(VoiceProfile.SUMMER_FEMALE, emptyList())

        assertTrue(resolved.isFallback)
        assertNull(resolved.engineVoiceId)
        assertFalse(resolved.profile.isAvailable)
    }

    // 11. Voice router selects natural local provider when genuinely available
    @Test
    fun testVoiceRouterSelectsNaturalLocalWhenAvailable() = runTest {
        val naturalProvider = FakeNaturalVoiceProvider(isAvailable = true)
        val systemFallback = FakeVoiceProvider(isAvailable = true)

        val router = VoiceEngineRouter(
            naturalVoiceProvider = naturalProvider,
            systemTtsProvider = systemFallback
        )

        assertEquals(VoiceProviderType.NATURAL_LOCAL, router.determineActiveProviderType())
        assertEquals(naturalProvider, router.getActiveProvider())
    }

    // 12. Voice router falls back to system offline provider when natural is not available
    @Test
    fun testVoiceRouterFallsBackToSystemOffline() = runTest {
        val naturalProvider = FakeNaturalVoiceProvider(isAvailable = false)
        val systemFallback = FakeVoiceProvider(isAvailable = true)

        val router = VoiceEngineRouter(
            naturalVoiceProvider = naturalProvider,
            systemTtsProvider = systemFallback
        )

        assertEquals(VoiceProviderType.SYSTEM_OFFLINE, router.determineActiveProviderType())
        assertEquals(systemFallback, router.getActiveProvider())
    }

    // 13. Voice router handles unavailable providers safely
    @Test
    fun testVoiceRouterHandlesUnavailableProviders() = runTest {
        val naturalProvider = FakeNaturalVoiceProvider(isAvailable = false)
        val systemFallback = FakeVoiceProvider(isAvailable = false)

        val router = VoiceEngineRouter(
            naturalVoiceProvider = naturalProvider,
            systemTtsProvider = systemFallback
        )

        assertFalse(router.isAvailable)
        val result = router.speak("Test text")
        assertFalse(result)
    }

    // 14. Speech queue preserves order
    @Test
    fun testSpeechQueuePreservesOrder() {
        val queue = SpeechQueue(maxQueueSize = 5)
        val item1 = SpeechItem(text = "First sentence", profile = VoiceProfile.SUMMER_FEMALE)
        val item2 = SpeechItem(text = "Second sentence", profile = VoiceProfile.SUMMER_FEMALE)

        queue.enqueue(item1)
        queue.enqueue(item2)

        assertEquals(2, queue.size)
        assertEquals("First sentence", queue.poll()?.text)
        assertEquals("Second sentence", queue.poll()?.text)
        assertTrue(queue.isEmpty)
    }

    // 15. Speech queue remains bounded
    @Test
    fun testSpeechQueueRemainsBounded() {
        val queue = SpeechQueue(maxQueueSize = 3)
        for (i in 1..10) {
            queue.enqueue(SpeechItem(text = "Sentence $i", profile = VoiceProfile.SUMMER_FEMALE))
        }

        assertEquals(3, queue.size)
        // Earliest items were evicted to prevent unbounded memory growth
        assertEquals("Sentence 8", queue.poll()?.text)
        assertEquals("Sentence 9", queue.poll()?.text)
        assertEquals("Sentence 10", queue.poll()?.text)
    }

    // 16. Conversational interruptibility clears queued speech
    @Test
    fun testConversationalInterruptibilityClearsQueue() {
        val queue = SpeechQueue(maxQueueSize = 5)
        queue.enqueue(SpeechItem(text = "Old 1", profile = VoiceProfile.SUMMER_FEMALE))
        queue.enqueue(SpeechItem(text = "Old 2", profile = VoiceProfile.SUMMER_FEMALE))

        val newItem = SpeechItem(text = "Fresh user response", profile = VoiceProfile.SUMMER_FEMALE)
        queue.interruptAndEnqueue(newItem)

        assertEquals(1, queue.size)
        assertEquals("Fresh user response", queue.poll()?.text)
    }

    // 17. Stop cancels speech safely
    @Test
    fun testStopCancelsActiveSpeechSafely() = runTest {
        val systemFallback = FakeVoiceProvider(isAvailable = true)
        val router = VoiceEngineRouter(systemTtsProvider = systemFallback)

        router.speak("This is an active speech statement.")
        router.stop()

        assertFalse(router.isSpeaking.value)
        assertTrue(
            router.speechState.value == SpeechState.STOPPED ||
                router.speechState.value == SpeechState.IDLE
        )
        assertTrue(systemFallback.stopCallCount >= 1)
        router.release()
    }

    // 18. Sentence segmentation does not split words unnecessarily
    @Test
    fun testSentenceSegmentationDoesNotSplitWords() {
        val segmenter = SpeechSegmenter(maxChunkLength = 60)
        val longText = "Hello user. This is a structured conversational sentence that should be chunked cleanly. Here is the second sentence."

        val chunks = segmenter.segment(longText)
        assertTrue(chunks.size >= 2)
        for (chunk in chunks) {
            assertFalse(chunk.endsWith(" "))
            assertFalse(chunk.startsWith(" "))
            assertTrue(chunk.length <= 60)
        }
    }

    // 19. Long responses are segmented safely
    @Test
    fun testLongResponsesSegmentedSafely() {
        val segmenter = SpeechSegmenter(maxChunkLength = 100)
        val response = "Summer is an observant personal AI companion running locally on Android. All data persistence, memory retrieval, and local reasoning remain entirely on-device without cloud dependence."

        val chunks = segmenter.segment(response)
        assertTrue(chunks.size >= 2)
        val reassembled = chunks.joinToString(" ")
        assertTrue(reassembled.contains("observant personal AI companion"))
        assertTrue(reassembled.contains("cloud dependence"))
    }

    // 20. Voice settings are clamped safely
    @Test
    fun testVoiceSettingsClampedSafely() {
        val router = VoiceEngineRouter()
        router.updateProfileSettings(
            speechRate = 99.0f, // crazy high
            pitch = -10.0f,     // crazy low
            volume = 5.0f       // crazy high
        )

        val updated = router.currentProfile.value
        assertNotNull(updated)
        assertEquals(2.0f, updated!!.speechRate, 0.01f) // clamped to 2.0f max
        assertEquals(0.5f, updated.pitch, 0.01f)      // clamped to 0.5f min
        assertEquals(1.0f, updated.volume, 0.01f)     // clamped to 1.0f max
    }

    // 21. Voice diagnostics accurately expose provider state without audio data
    @Test
    fun testVoiceDiagnosticsExposeProviderState() {
        val systemFallback = FakeVoiceProvider(isAvailable = true)
        val router = VoiceEngineRouter(systemTtsProvider = systemFallback)
        router.refreshDiagnostics()

        val diag = router.diagnostics.value
        assertTrue(diag.systemActive)
        assertEquals(VoiceProviderType.SYSTEM_OFFLINE, diag.activeProvider)
        assertEquals(VoiceProfileId.FEMALE, diag.selectedProfile)
        assertTrue(diag.isOfflineCapable)
        assertFalse(diag.isNaturalNeuralAvailable) // truthfully reports false
    }
}
