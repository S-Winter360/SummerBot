package com.example.memory.models

/**
 * Core settings governing Summer's operational boundaries and capability activations.
 * Backed by local persistent storage.
 */
data class SummerSettings(
    val summerEnabled: Boolean = true,
    val voiceInteractionEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val proactiveResponsesEnabled: Boolean = true,
    val internetAccessAllowed: Boolean = false,
    val cameraAccessAllowed: Boolean = false,
    val personalMemoryEnabled: Boolean = true,
    val learningEnabled: Boolean = true,
    val speakerRecognitionEnabled: Boolean = false
)
