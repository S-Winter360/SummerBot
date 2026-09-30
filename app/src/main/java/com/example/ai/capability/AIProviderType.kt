package com.example.ai.capability

enum class AIProviderType(val displayName: String) {
    ON_DEVICE_GENAI("On-Device GenAI"),
    EMBEDDED_LOCAL_MODEL("Embedded Local Model"),
    DETERMINISTIC_LOCAL("Deterministic Local"),
    CLOUD("Cloud AI")
}
