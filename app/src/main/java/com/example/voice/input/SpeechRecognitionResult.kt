package com.example.voice.input

/**
 * Structured outcome produced by speech recognition providers.
 */
sealed interface SpeechRecognitionResult {
    data class Success(
        val transcript: String,
        val confidence: Float? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val locale: String? = null
    ) : SpeechRecognitionResult

    data class NoSpeech(
        val reason: String = "No speech input was detected."
    ) : SpeechRecognitionResult

    data class NoMatch(
        val reason: String = "Could not recognize what was spoken."
    ) : SpeechRecognitionResult

    data class PermissionDenied(
        val reason: String = "Microphone permission is not granted."
    ) : SpeechRecognitionResult

    data class Unavailable(
        val reason: String = "Speech recognition service is unavailable."
    ) : SpeechRecognitionResult

    data class Error(
        val error: SpeechRecognitionError,
        val message: String,
        val throwable: Throwable? = null
    ) : SpeechRecognitionResult
}
