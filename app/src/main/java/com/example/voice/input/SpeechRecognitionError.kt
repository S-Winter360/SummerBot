package com.example.voice.input

/**
 * Standardized categorization of speech recognition failures.
 * Shields UI layers from implementation-specific platform exception details.
 */
enum class SpeechRecognitionError(val userMessage: String) {
    PERMISSION_DENIED("Microphone permission was not granted."),
    SERVICE_UNAVAILABLE("Speech recognition service is unavailable on this device."),
    NETWORK_REQUIRED("Speech recognition requires network connectivity on this configuration."),
    NO_SPEECH("No speech input was detected."),
    NO_MATCH("Could not recognize what was spoken."),
    AUDIO_ERROR("An audio recording error occurred."),
    CLIENT_ERROR("A client error occurred in speech recognition."),
    BUSY("Speech recognition service is currently busy."),
    UNKNOWN("An unknown speech recognition error occurred.")
}
