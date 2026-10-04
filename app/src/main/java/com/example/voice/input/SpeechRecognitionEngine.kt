package com.example.voice.input

import kotlinx.coroutines.flow.StateFlow

/**
 * Isolated contract for speech-to-text input recognition.
 * Must NOT depend on Compose UI or directly invoke SummerOrchestrator.
 */
interface SpeechRecognitionEngine {
    val state: StateFlow<SpeechRecognitionState>
    val partialTranscript: StateFlow<String>
    val diagnostics: StateFlow<SpeechRecognitionDiagnostics>

    suspend fun startListening(onResult: (SpeechRecognitionResult) -> Unit)
    suspend fun stopListening()
    fun cancel()
    fun release()
}
