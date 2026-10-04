package com.example.vision

/**
 * Classification of vision processing providers.
 * In Phase 0G, [STUB] is the sole functional provider for architectural validation.
 * Other types represent future on-device integration targets.
 */
enum class VisionProviderType {
    ANDROID_CAMERA,
    LOCAL_VISION,
    ON_DEVICE_GENAI,
    OPTIONAL_ONLINE,
    STUB
}
