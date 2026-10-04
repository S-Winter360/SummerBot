package com.example.vision

import java.util.UUID

enum class VisionInputSource {
    CAMERA,
    IMAGE,
    SCREEN,
    OTHER
}

/**
 * Architectural container for future visual inputs.
 * Strictly ephemeral: never persisted to Room, never uploaded, never cached beyond active request.
 */
data class VisionInput(
    val id: String = UUID.randomUUID().toString(),
    val source: VisionInputSource = VisionInputSource.CAMERA,
    val timestamp: Long = System.currentTimeMillis(),
    val width: Int? = null,
    val height: Int? = null,
    val metadata: Map<String, String> = emptyMap(),
    val tag: String = ""
)
