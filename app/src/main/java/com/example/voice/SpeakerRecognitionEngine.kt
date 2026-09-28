package com.example.voice

/**
 * Result of biometric voiceprint verification against stored user profiles.
 */
sealed interface SpeakerVerificationResult {
    data class Match(
        val userId: String,
        val displayName: String,
        val confidence: Float
    ) : SpeakerVerificationResult

    data object UnknownSpeaker : SpeakerVerificationResult

    data class Error(val message: String) : SpeakerVerificationResult
}

/**
 * Abstraction for local speaker identification and voice enrollment.
 */
interface SpeakerRecognitionEngine {
    val isTrained: Boolean

    suspend fun verifySpeaker(audioSample: ByteArray): SpeakerVerificationResult

    suspend fun enrollSpeaker(userId: String, displayName: String, audioSamples: List<ByteArray>): Result<Unit>
}
