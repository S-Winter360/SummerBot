package com.example.vision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat

interface VisionCapabilityDetector {
    fun isCameraHardwarePresent(): Boolean
    fun hasFrontCamera(): Boolean
    fun hasRearCamera(): Boolean
    fun getCameraPermissionState(): CameraPermissionState
    fun detectDiagnostics(activeProvider: VisionProviderType, visionState: VisionState): VisionDiagnostics
}

class AndroidVisionCapabilityDetector(
    private val context: Context,
    private val cameraManagerProvider: () -> CameraManager? = {
        try {
            context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } catch (_: Throwable) {
            null
        }
    }
) : VisionCapabilityDetector {

    override fun isCameraHardwarePresent(): Boolean {
        val pm = context.packageManager ?: return false
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }

    override fun hasFrontCamera(): Boolean {
        val pm = context.packageManager
        if (pm != null && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return true
        }
        return try {
            val cm = cameraManagerProvider() ?: return false
            cm.cameraIdList.any { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            }
        } catch (_: Throwable) {
            false
        }
    }

    override fun hasRearCamera(): Boolean {
        val pm = context.packageManager
        if (pm != null && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true
        }
        return try {
            val cm = cameraManagerProvider() ?: return false
            cm.cameraIdList.any { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (_: Throwable) {
            false
        }
    }

    override fun getCameraPermissionState(): CameraPermissionState {
        return try {
            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (status == PackageManager.PERMISSION_GRANTED) {
                CameraPermissionState.GRANTED
            } else {
                CameraPermissionState.DENIED
            }
        } catch (_: Throwable) {
            CameraPermissionState.UNKNOWN
        }
    }

    override fun detectDiagnostics(
        activeProvider: VisionProviderType,
        visionState: VisionState
    ): VisionDiagnostics {
        val hasHw = isCameraHardwarePresent()
        val perm = getCameraPermissionState()
        return VisionDiagnostics(
            cameraHardwareAvailable = hasHw,
            frontCameraAvailable = hasFrontCamera(),
            rearCameraAvailable = hasRearCamera(),
            cameraPermissionState = perm,
            activeProviderType = activeProvider,
            processingStatus = "NOT_IMPLEMENTED",
            supportedCapabilities = setOf(VisionCapability.IMAGE_ANALYSIS),
            visionState = visionState,
            lastCheckedTimestamp = System.currentTimeMillis(),
            isLiveVisionActive = false,
            message = "Camera hardware: ${if (hasHw) "Available" else "Unavailable"}. Live vision pipeline is not yet implemented."
        )
    }
}
