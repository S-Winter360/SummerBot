package com.example.voice.input

import java.util.Locale

/**
 * Verified capability status of the local speech recognition engine.
 */
enum class SpeechOfflineCapability(val label: String) {
    ON_DEVICE_CONFIRMED("On-Device Confirmed"),
    NETWORK_DEPENDENT("Network Dependent"),
    UNKNOWN("Unknown / Platform Managed")
}

/**
 * Diagnostic summary of the active speech recognition system.
 * Truthful reporting: never claims offline functionality unless verified by provider.
 */
data class SpeechRecognitionDiagnostics(
    val recognizerName: String = "Android SpeechRecognizer",
    val isAvailable: Boolean = true,
    val microphonePermissionGranted: Boolean = false,
    val locale: String = try { Locale.getDefault().toLanguageTag() } catch (_: Throwable) { "en-US" },
    val offlineCapability: SpeechOfflineCapability = SpeechOfflineCapability.UNKNOWN,
    val state: SpeechRecognitionState = SpeechRecognitionState.IDLE,
    val isListening: Boolean = false,
    val lastError: String? = null,
    val message: String = "Local speech input system ready."
)
