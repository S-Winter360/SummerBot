package com.example.ai.capability

import com.example.ai.localmodel.EmbeddedModelDiagnostics
import com.example.ai.localmodel.EmbeddedModelStatus
import com.example.vision.VisionDiagnostics
import com.example.voice.input.SpeechRecognitionDiagnostics
import com.example.voice.models.VoiceDiagnostics

/**
 * Concise diagnostics summary of Summer's active AI configuration, capability status, memory, voice, vision, and speech-input subsystems.
 * Distinguishes between on-device GenAI (Gemini Nano), embedded local model (LiteRT-LM), deterministic fallback, and overall effective local AI availability.
 */
data class AIDiagnostics(
    val detectedProvider: AIProviderType = AIProviderType.ON_DEVICE_GENAI,
    val runtimeStatus: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING,
    val effectiveLocalAIStatus: AIAvailabilityStatus = runtimeStatus,
    val onDeviceGenAIStatus: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING,
    val embeddedModelStatus: EmbeddedModelStatus = EmbeddedModelStatus.NOT_INSTALLED,
    val deterministicStatus: AIAvailabilityStatus = AIAvailabilityStatus.AVAILABLE,
    val activeProvider: AIProviderType = AIProviderType.DETERMINISTIC_LOCAL,
    val currentModel: String = "Summer Offline Core",
    val activeRuntime: String = "Summer Offline Core",
    val activeQuantization: String? = null,
    val executionMode: String = "Offline",
    val supportedCapabilities: List<AICapability> = listOf(
        AICapability.CHAT,
        AICapability.STRUCTURED_OUTPUT,
        AICapability.TEXT_GENERATION
    ),
    val isFallbackActive: Boolean = true,
    val embeddedModelDiagnostics: EmbeddedModelDiagnostics = EmbeddedModelDiagnostics(),
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
    val lastMemoryOperation: String = "NONE",
    val voiceDiagnostics: VoiceDiagnostics = VoiceDiagnostics(),
    val visionDiagnostics: VisionDiagnostics = VisionDiagnostics(),
    val speechDiagnostics: SpeechRecognitionDiagnostics = SpeechRecognitionDiagnostics()
) {
    // Backward-compatible properties
    val currentProvider: AIProviderType get() = activeProvider
    val providerAvailability: AIAvailabilityStatus get() = runtimeStatus
    val modelAvailability: AIAvailabilityStatus get() = runtimeStatus
    val effectiveStatus: AIAvailabilityStatus get() = effectiveLocalAIStatus
}
