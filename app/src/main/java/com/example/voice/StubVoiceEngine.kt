package com.example.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Architectural foundation stub for VoiceEngine.
 * Intentionally defers live microphone capture and synthesis to later phases
 * while fulfilling the architectural contract.
 */
class StubVoiceEngine : VoiceEngine {
    override val isInitialized: Boolean = true

    override suspend fun startListening(): Flow<VoiceEvent> {
        // Safe foundation stub: does not activate unauthorized background hardware
        return emptyFlow()
    }

    override suspend fun stopListening() {
        // No-op in architectural foundation phase
    }

    override suspend fun speak(utterance: String): VoiceSynthesisResult {
        return VoiceSynthesisResult(
            isSuccess = true,
            durationMs = (utterance.length * 50L).coerceAtLeast(300L),
            note = "Voice synthesis engine architectural foundation ready."
        )
    }
}

/**
 * Architectural foundation stub for SpeakerRecognitionEngine.
 * Intentionally defers voiceprint registration to future biometric phases.
 */
class StubSpeakerRecognitionEngine : SpeakerRecognitionEngine {
    override val isTrained: Boolean = false

    override suspend fun verifySpeaker(audioSample: ByteArray): SpeakerVerificationResult {
        return SpeakerVerificationResult.UnknownSpeaker
    }

    override suspend fun enrollSpeaker(
        userId: String,
        displayName: String,
        audioSamples: List<ByteArray>
    ): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Speaker enrollment is deferred to future biometric phase.")
        )
    }
}
