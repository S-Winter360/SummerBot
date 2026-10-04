package com.example.voice.input

/**
 * Strongly-typed lifecycle state for local speech recognition (STT) input.
 * Distinct from global [com.example.core.state.SummerState] and TTS output states.
 */
enum class SpeechRecognitionState {
    IDLE,
    INITIALIZING,
    LISTENING,
    PROCESSING,
    COMPLETED,
    ERROR,
    STOPPED,
    UNAVAILABLE
}
