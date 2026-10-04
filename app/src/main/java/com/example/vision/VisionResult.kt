package com.example.vision

/**
 * Strongly-typed outcome of a vision processing request.
 * Encapsulates failures and policy blocks gracefully without exposing raw exceptions to the UI.
 */
sealed interface VisionResult {
    data class Success(
        val observation: VisionObservation
    ) : VisionResult

    data class NoObservation(
        val reason: String = "No observation produced from visual input"
    ) : VisionResult

    data class Unavailable(
        val reason: String = "Vision provider unavailable"
    ) : VisionResult

    data class PermissionDenied(
        val reason: String = "Camera permission not granted"
    ) : VisionResult

    data class Unsupported(
        val reason: String = "Vision capability not supported on this platform"
    ) : VisionResult

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : VisionResult
}
