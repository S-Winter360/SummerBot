package com.example.ai.capability

data class AIModelMetadata(
    val provider: AIProviderType,
    val modelIdentifier: String,
    val displayName: String,
    val modelVersion: String? = null,
    val availabilityStatus: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING,
    val supportedCapabilities: Set<AICapability> = emptySet(),
    val offlineCapable: Boolean = true,
    val multimodalSupport: Boolean = false,
    val maxInputTokens: Int? = null,
    val maxOutputTokens: Int? = null,
    val isStreamingSupported: Boolean? = null,
    val deviceRequirements: String? = null,
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
)
