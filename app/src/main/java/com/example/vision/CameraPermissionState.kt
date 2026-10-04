package com.example.vision

/**
 * Abstraction representing camera runtime permission status.
 * Evaluated without triggering Android permission dialogs from the core engine.
 */
enum class CameraPermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}
