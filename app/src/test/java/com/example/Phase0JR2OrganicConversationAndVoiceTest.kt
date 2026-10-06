package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.components.formatTranscriptForClipboard
import com.example.ui.models.ConversationMessage
import com.example.ui.models.ConversationSource
import com.example.ui.models.ConversationSpeaker
import com.example.voice.engine.VoiceProfileResolver
import com.example.voice.models.VoiceCharacteristics
import com.example.voice.models.VoiceGender
import com.example.voice.models.VoiceProfile
import com.example.voice.models.VoiceProfileId
import com.example.voice.provider.AndroidSystemTtsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Validates Phase 0J-R2 requirements:
 * 1. Living AI core tap state machine & interactions
 * 2. In-session persistent conversation transcript data model & formatting
 * 3. Developer clipboard transcript formatting
 * 4. Corrected female & male voice profiles, pitches, and characteristics
 * 5. Deterministic voice profile resolution preventing male/female inversion
 */
@RunWith(RobolectricTestRunner::class)
class Phase0JR2OrganicConversationAndVoiceTest {

    @Test
    fun testVoiceProfiles_MaleAndFemalePitchesAndCharacteristicsNotSwapped() {
        val femaleProfile = VoiceProfile.SUMMER_FEMALE
        val maleProfile = VoiceProfile.SUMMER_MALE

        // Female profile must have female gender and higher pitch (~1.05x)
        assertEquals(VoiceProfileId.FEMALE, femaleProfile.id)
        assertEquals(VoiceGender.FEMALE, femaleProfile.gender)
        assertTrue("Female pitch should be higher than 1.0f", femaleProfile.pitch >= 1.0f)
        assertTrue(femaleProfile.targetCharacteristics.description.contains("female", ignoreCase = true))

        // Male profile must have male gender and deeper pitch (~0.85x)
        assertEquals(VoiceProfileId.MALE, maleProfile.id)
        assertEquals(VoiceGender.MALE, maleProfile.gender)
        assertTrue("Male pitch should be lower than 1.0f", maleProfile.pitch < 1.0f)
        assertTrue(maleProfile.targetCharacteristics.description.contains("male", ignoreCase = true))

        // Ensure pitch difference is in the right direction
        assertTrue(femaleProfile.pitch > maleProfile.pitch)
    }

    @Test
    fun testVoiceProfileResolver_CorrectlyResolvesMaleAndFemaleVoices() {
        val resolver = VoiceProfileResolver()
        val availableVoices = listOf(
            "en-us-x-sfg#female_2",
            "en-us-x-tpd#male_1"
        )

        val femaleResult = resolver.resolve(VoiceProfile.SUMMER_FEMALE, availableVoices)
        assertTrue(femaleResult.isExactGenderMatch)
        assertEquals("en-us-x-sfg#female_2", femaleResult.engineVoiceId)

        val maleResult = resolver.resolve(VoiceProfile.SUMMER_MALE, availableVoices)
        assertTrue(maleResult.isExactGenderMatch)
        assertEquals("en-us-x-tpd#male_1", maleResult.engineVoiceId)
    }

    @Test
    fun testAndroidSystemTtsProvider_HeuristicGenderMatching() {
        val provider = AndroidSystemTtsProvider(ApplicationProvider.getApplicationContext())

        // Create mock Voice items
        val femaleVoice = android.speech.tts.Voice(
            "en-us-x-sfg#female",
            java.util.Locale.US,
            android.speech.tts.Voice.QUALITY_NORMAL,
            android.speech.tts.Voice.LATENCY_NORMAL,
            false,
            emptySet()
        )
        val maleVoice = android.speech.tts.Voice(
            "en-us-x-tpd#male",
            java.util.Locale.US,
            android.speech.tts.Voice.QUALITY_NORMAL,
            android.speech.tts.Voice.LATENCY_NORMAL,
            false,
            emptySet()
        )

        val voiceSet = setOf(femaleVoice, maleVoice)

        val matchedFemale = provider.findBestMatchingVoice(voiceSet, VoiceProfile.SUMMER_FEMALE)
        assertNotNull(matchedFemale)
        assertEquals("en-us-x-sfg#female", matchedFemale?.name)

        val matchedMale = provider.findBestMatchingVoice(voiceSet, VoiceProfile.SUMMER_MALE)
        assertNotNull(matchedMale)
        assertEquals("en-us-x-tpd#male", matchedMale?.name)
    }

    @Test
    fun testConversationTranscript_MessageModelAndFormatting() {
        val msg1 = ConversationMessage(
            speaker = ConversationSpeaker.USER,
            text = "Good morning Summer",
            source = ConversationSource.VOICE
        )
        val msg2 = ConversationMessage(
            speaker = ConversationSpeaker.SUMMER,
            text = "Good morning. How can I help you today?",
            source = ConversationSource.SYSTEM
        )

        val messages = listOf(msg1, msg2)
        val formatted = formatTranscriptForClipboard(messages)

        assertTrue(formatted.contains("SUMMER CONVERSATION TRANSCRIPT"))
        assertTrue(formatted.contains("User (VOICE):"))
        assertTrue(formatted.contains("Good morning Summer"))
        assertTrue(formatted.contains("Summer (SYSTEM):"))
        assertTrue(formatted.contains("Good morning. How can I help you today?"))
        assertTrue(formatted.contains("Total turns: 2"))
    }

    @Test
    fun testMainViewModel_LivingCircleClickStateTransitions() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = MainViewModel(app)

        // Initial message present in transcript
        val initialMessages = vm.conversationMessages.value
        assertTrue(initialMessages.isNotEmpty())
        assertEquals(ConversationSpeaker.SUMMER, initialMessages.first().speaker)

        // Tapping living circle in Idle starts listening
        vm.onCoreOrbClick()
        // SummerState becomes Listening
        assertTrue(vm.summerState.value is com.example.core.state.SummerState.Listening)

        // Tapping living circle while listening stops listening
        vm.onCoreOrbClick()
        // No crash, state transition managed
    }
}
