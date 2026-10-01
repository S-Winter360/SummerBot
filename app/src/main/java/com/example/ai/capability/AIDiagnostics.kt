package com.example.ai.capability

/**
 * Concise diagnostics summary of Summer's active AI configuration, capability status, and memory subsystem.
 * Intended for presentation in settings and development inspection.
 * Distinguishes between detected provider/status, the active runtime provider, and local memory metrics.
 */
data class AIDiagnostics(
    val detectedProvider: AIProviderType = AIProviderType.ON_DEVICE_GENAI,
    val runtimeStatus: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING,
    val activeProvider: AIProviderType = AIProviderType.DETERMINISTIC_LOCAL,
    val currentModel: String = "Summer Offline Core",
    val supportedCapabilities: List<AICapability> = listOf(
        AICapability.CHAT,
        AICapability.STRUCTURED_OUTPUT,
        AICapability.TEXT_GENERATION
    ),
    val isFallbackActive: Boolean = true,
    val deviceApiLevel: Int = try { android.os.Build.VERSION.SDK_INT } catch (_: Throwable) { 0 },
    val deviceManufacturer: String = try { android.os.Build.MANUFACTURER ?: "Generic" } catch (_: Throwable) { "Generic" },
    val deviceModel: String = try { android.os.Build.MODEL ?: "Device" } catch (_: Throwable) { "Device" },
    val isNetworkAvailable: Boolean = false,
    val isAiCoreInstalled: Boolean = false,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val memorySystemActive: Boolean = true,
    val persistentMemoryCount: Int = 0,
    val recentTurnCount: Int = 0,
    val lastRetrievedMemoryCount: Int = 0,
    val lastMemoryOperation: String = "NONE"
) {
    // Backward-compatible properties
    val currentProvider: AIProviderType get() = activeProvider
    val providerAvailability: AIAvailabilityStatus get() = runtimeStatus
    val modelAvailability: AIAvailabilityStatus get() = runtimeStatus
}
