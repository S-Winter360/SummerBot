package com.example.vision

/**
 * Result of visual contextual reasoning.
 */
data class VisionAnalysisResult(
    val detectedObjects: List<String> = emptyList(),
    val summary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Abstraction for camera frame analysis and visual understanding.
 * Future phases will bind this to on-device LiteRT/TensorFlow Lite or on-device vision models.
 */
interface VisionEngine {
    val isCameraAvailable: Boolean

    suspend fun analyzeFrame(frameData: ByteArray): VisionAnalysisResult

    suspend fun startContextualObservation()

    suspend fun stopContextualObservation()
}

/**
 * Architectural foundation stub for VisionEngine.
 * Complies with strict privacy mandate: zero camera surveillance in this phase.
 */
class StubVisionEngine : VisionEngine {
    override val isCameraAvailable: Boolean = false

    override suspend fun analyzeFrame(frameData: ByteArray): VisionAnalysisResult {
        return VisionAnalysisResult(
            detectedObjects = emptyList(),
            summary = "Vision engine foundation active. Visual analysis deferred to vision phase."
        )
    }

    override suspend fun startContextualObservation() {
        // Deferred intentionally
    }

    override suspend fun stopContextualObservation() {
        // Deferred intentionally
    }
}
