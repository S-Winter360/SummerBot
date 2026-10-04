package com.example.vision

/**
 * Isolated vision processing provider contract.
 * Must NOT manipulate Compose UI, access SummerOrchestrator, write to Room DAOs,
 * or execute Android actions directly.
 */
interface VisionProvider {
    val providerType: VisionProviderType
    val isAvailable: Boolean
    val supportedCapabilities: Set<VisionCapability>

    suspend fun process(input: VisionInput): VisionResult
    suspend fun stop()
    fun release()
}
