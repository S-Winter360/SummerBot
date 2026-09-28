package com.example.voice

import kotlinx.coroutines.flow.Flow

/**
 * Speech recognition and synthesis events.
 */
sealed interface VoiceEvent {
    data object ListeningStarted : VoiceEvent
    data class PartialRecognition(val hypothesis: String) : VoiceEvent
    data class FinalRecognition(val transcript: String, val confidence: Float) : VoiceEvent
    data object ListeningStopped : VoiceEvent
    data class Error(val message: String) : VoiceEvent
}

/**
 * Result of speech synthesis operations.
 */
data class VoiceSynthesisResult(
    val isSuccess: Boolean,
    val durationMs: Long = 0L,
    val note: String = ""
)

/**
 * Core abstraction for offline speech recognition and vocal synthesis.
 * Future phases will bind this to on-device Vosk, Whisper, or Android SpeechRecognizer.
 */
interface VoiceEngine {
    val isInitialized: Boolean

    suspend fun startListening(): Flow<VoiceEvent>

    suspend fun stopListening()

    suspend fun speak(utterance: String): VoiceSynthesisResult
}
