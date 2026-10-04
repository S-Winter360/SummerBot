package com.example.vision

/**
 * Transparent diagnostic summary of camera hardware, runtime permissions, and active vision engine.
 * Truthful reporting: distinguishes hardware presence from active processing capabilities.
 */
data class VisionDiagnostics(
    val cameraHardwareAvailable: Boolean = false,
    val frontCameraAvailable: Boolean = false,
    val rearCameraAvailable: Boolean = false,
    val cameraPermissionState: CameraPermissionState = CameraPermissionState.DENIED,
    val activeProviderType: VisionProviderType = VisionProviderType.STUB,
    val processingStatus: String = "NOT_IMPLEMENTED", // AVAILABLE, NOT_IMPLEMENTED, UNSUPPORTED
    val supportedCapabilities: Set<VisionCapability> = setOf(VisionCapability.IMAGE_ANALYSIS),
    val visionState: VisionState = VisionState.IDLE,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val isLiveVisionActive: Boolean = false,
    val message: String = "Vision architectural foundation active. Live camera capture is not implemented in this phase."
)
