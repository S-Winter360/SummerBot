package com.example.vision

/**
 * Deterministic local stub provider for architectural testing.
 * Truthfully reports stub nature and never fabricates real-world object/scene detections.
 */
class StubVisionProvider(
    override var isAvailable: Boolean = true
) : VisionProvider {

    override val providerType: VisionProviderType = VisionProviderType.STUB

    override val supportedCapabilities: Set<VisionCapability> = setOf(
        VisionCapability.IMAGE_ANALYSIS
    )

    override suspend fun process(input: VisionInput): VisionResult {
        if (!isAvailable) {
            return VisionResult.Unavailable("Stub vision provider is currently marked unavailable.")
        }

        // Truthful, deterministic result clearly identified as an architectural stub
        return VisionResult.Success(
            observation = VisionObservation(
                timestamp = System.currentTimeMillis(),
                detectedObjects = emptyList(),
                sceneDescription = "Vision processing is not currently enabled (Architectural stub active).",
                detectedText = "",
                environmentalObservations = "",
                confidence = 0.0f,
                source = input.source,
                isSyntheticOrStub = true
            )
        )
    }

    override suspend fun stop() {
        // No-op for deterministic stub
    }

    override fun release() {
        // No-op for deterministic stub
    }
}
