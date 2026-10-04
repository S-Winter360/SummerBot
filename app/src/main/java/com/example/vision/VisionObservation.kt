package com.example.vision

/**
 * Structured observation produced by a vision provider.
 * Does not contain raw image data or bitmaps.
 */
data class VisionObservation(
    val timestamp: Long = System.currentTimeMillis(),
    val detectedObjects: List<String> = emptyList(),
    val sceneDescription: String = "",
    val detectedText: String = "",
    val environmentalObservations: String = "",
    val confidence: Float = 0.0f,
    val source: VisionInputSource = VisionInputSource.CAMERA,
    val isSyntheticOrStub: Boolean = false
) {
    /**
     * Bounded, safe textual summary suitable for [com.example.core.context.SummerContext].
     */
    fun toContextSummary(): String {
        return buildString {
            if (sceneDescription.isNotBlank()) append(sceneDescription)
            if (detectedObjects.isNotEmpty()) {
                if (isNotEmpty()) append("; ")
                append("Objects: ").append(detectedObjects.joinToString())
            }
            if (detectedText.isNotBlank()) {
                if (isNotEmpty()) append("; ")
                append("Text: ").append(detectedText.take(100))
            }
        }.ifBlank { "No visual observations recorded" }
    }
}
