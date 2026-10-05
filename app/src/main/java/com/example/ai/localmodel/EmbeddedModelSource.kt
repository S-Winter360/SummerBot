package com.example.ai.localmodel

/**
 * Supported sources for acquiring embedded local generative language models.
 */
enum class EmbeddedModelSource(val displayName: String) {
    LOCAL_IMPORT("Local Storage Import"),
    AUTHENTICATED_REMOTE("Authenticated Remote Repository"),
    PLAY_DELIVERED("Google Play Asset Delivery"),
    BUNDLED("Application Asset Package")
}
