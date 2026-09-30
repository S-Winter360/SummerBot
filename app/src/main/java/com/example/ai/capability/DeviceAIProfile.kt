package com.example.ai.capability

data class DeviceAIProfile(
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val totalMemoryBytes: Long? = null,
    val availableMemoryBytes: Long? = null,
    val totalStorageBytes: Long? = null,
    val availableStorageBytes: Long? = null,
    val isNetworkAvailable: Boolean = false,
    val onDeviceGenAIAvailability: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING,
    val isAiCoreInstalled: Boolean = false,
    val detectedProviders: List<AIProviderType> = listOf(AIProviderType.DETERMINISTIC_LOCAL),
    val recommendedProvider: AIProviderType = AIProviderType.DETERMINISTIC_LOCAL,
    val checkTimestamp: Long = System.currentTimeMillis()
)
