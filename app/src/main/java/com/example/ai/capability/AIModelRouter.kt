package com.example.ai.capability

import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.localmodel.EmbeddedModelDiagnostics
import com.example.ai.localmodel.EmbeddedModelManager
import com.example.ai.localmodel.EmbeddedModelStatus
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Intelligent routing layer for AI execution in Summer.
 * Decouples [com.example.core.orchestrator.SummerOrchestrator] from concrete AI providers.
 *
 * Hierarchy:
 * 1. ON_DEVICE_GENAI (Gemini Nano) if AIAvailabilityStatus == AVAILABLE
 * 2. EMBEDDED_LOCAL_MODEL (Google LiteRT-LM / Gemma 3 1B IT) if status == READY
 * 3. DETERMINISTIC_LOCAL (Summer Offline Core) as safe baseline fallback
 */
class AIModelRouter(
    val registry: AIModelRegistry,
    val fallbackEngine: OfflineLocalAIEngine,
    private val detector: DeviceAICapabilityDetector? = null,
    private val networkProvider: NetworkInformationProvider? = null,
    private val embeddedModelManager: EmbeddedModelManager? = null,
    private val embeddedEngine: AIEngine? = null,
    private val routerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : AIEngine {

    companion object {
        private const val TAG = "AIModelRouter"
    }

    private val _diagnostics = MutableStateFlow(
        AIDiagnostics(
            detectedProvider = AIProviderType.ON_DEVICE_GENAI,
            runtimeStatus = AIAvailabilityStatus.CHECKING,
            activeProvider = AIProviderType.DETERMINISTIC_LOCAL,
            currentModel = fallbackEngine.modelInfo.name,
            isFallbackActive = true,
            supportedCapabilities = listOf(
                AICapability.CHAT,
                AICapability.STRUCTURED_OUTPUT,
                AICapability.TEXT_GENERATION
            )
        )
    )
    val diagnostics: StateFlow<AIDiagnostics> = _diagnostics.asStateFlow()

    init {
        // Register deterministic fallback by default
        registry.register(
            providerType = AIProviderType.DETERMINISTIC_LOCAL,
            engine = fallbackEngine,
            metadata = AIModelMetadata(
                provider = AIProviderType.DETERMINISTIC_LOCAL,
                modelIdentifier = "summer-offline-core",
                displayName = fallbackEngine.modelInfo.name,
                modelVersion = fallbackEngine.modelInfo.version,
                availabilityStatus = AIAvailabilityStatus.AVAILABLE,
                supportedCapabilities = setOf(
                    AICapability.CHAT,
                    AICapability.STRUCTURED_OUTPUT,
                    AICapability.TEXT_GENERATION
                ),
                offlineCapable = true,
                multimodalSupport = false
            )
        )

        // Register embedded engine if provided
        if (embeddedEngine != null) {
            val isReady = embeddedModelManager?.isModelReady() ?: embeddedEngine.isReady
            registry.register(
                providerType = AIProviderType.EMBEDDED_LOCAL_MODEL,
                engine = embeddedEngine,
                metadata = AIModelMetadata(
                    provider = AIProviderType.EMBEDDED_LOCAL_MODEL,
                    modelIdentifier = "gemma-3-1b-it-litertlm",
                    displayName = embeddedEngine.modelInfo.name,
                    modelVersion = embeddedEngine.modelInfo.version,
                    availabilityStatus = if (isReady) AIAvailabilityStatus.AVAILABLE else AIAvailabilityStatus.UNAVAILABLE,
                    supportedCapabilities = setOf(
                        AICapability.CHAT,
                        AICapability.STRUCTURED_OUTPUT,
                        AICapability.TEXT_GENERATION,
                        AICapability.SUMMARIZATION
                    ),
                    offlineCapable = true,
                    multimodalSupport = false
                )
            )
        }

        // Observe embedded model manager status changes
        if (embeddedModelManager != null) {
            routerScope.launch {
                embeddedModelManager.status.collect { status ->
                    val availability = if (status == EmbeddedModelStatus.READY) {
                        AIAvailabilityStatus.AVAILABLE
                    } else {
                        AIAvailabilityStatus.UNAVAILABLE
                    }
                    registry.updateAvailability(AIProviderType.EMBEDDED_LOCAL_MODEL, availability)
                    val (selectedType, _) = route()
                    updateDiagnostics(selectedType, null)
                }
            }
        }

        // Asynchronously run initial capability check
        if (detector != null) {
            routerScope.launch {
                refreshCapabilities()
            }
        }
    }

    /**
     * Dynamically chooses the appropriate AI provider for a required [AICapability].
     * 1. ON_DEVICE_GENAI if Gemini Nano is AVAILABLE.
     * 2. EMBEDDED_LOCAL_MODEL if embedded model is READY / AVAILABLE.
     * 3. DETERMINISTIC_LOCAL fallback.
     */
    fun route(capability: AICapability = AICapability.TEXT_GENERATION): Pair<AIProviderType, AIEngine> {
        val onDeviceMeta = registry.getMetadata(AIProviderType.ON_DEVICE_GENAI)
        val onDeviceEngine = registry.getEngine(AIProviderType.ON_DEVICE_GENAI)

        if (onDeviceMeta != null &&
            onDeviceMeta.availabilityStatus == AIAvailabilityStatus.AVAILABLE &&
            capability in onDeviceMeta.supportedCapabilities &&
            onDeviceEngine != null &&
            onDeviceEngine.isReady
        ) {
            logInfo("Routing capability [$capability] to On-Device GenAI provider.")
            return Pair(AIProviderType.ON_DEVICE_GENAI, onDeviceEngine)
        }

        val embeddedMeta = registry.getMetadata(AIProviderType.EMBEDDED_LOCAL_MODEL)
        val engine = registry.getEngine(AIProviderType.EMBEDDED_LOCAL_MODEL) ?: embeddedEngine
        val isEmbeddedReady = embeddedModelManager?.isModelReady() ?: (engine?.isReady == true)

        if (embeddedMeta != null &&
            (embeddedMeta.availabilityStatus == AIAvailabilityStatus.AVAILABLE || isEmbeddedReady) &&
            capability in embeddedMeta.supportedCapabilities &&
            engine != null &&
            isEmbeddedReady
        ) {
            logInfo("Routing capability [$capability] to Embedded Local Model provider.")
            return Pair(AIProviderType.EMBEDDED_LOCAL_MODEL, engine)
        }

        // Guaranteed deterministic local fallback
        logInfo("Routing capability [$capability] to Deterministic Local fallback.")
        return Pair(AIProviderType.DETERMINISTIC_LOCAL, fallbackEngine)
    }

    /**
     * Executes non-blocking capability detection, updates the registry, and publishes diagnostics.
     */
    suspend fun refreshCapabilities(): DeviceAIProfile? {
        logInfo("Triggering AI capability detection refresh...")
        _diagnostics.update { it.copy(runtimeStatus = AIAvailabilityStatus.CHECKING) }

        if (detector == null) {
            logInfo("No detector provided; evaluating active route.")
            val (selectedType, _) = route()
            updateDiagnostics(selectedType, null)
            return null
        }

        return try {
            val profile = detector.detect()
            logInfo("Received device AI profile: GenAI status=${profile.onDeviceGenAIAvailability.name}")

            registry.updateAvailability(
                AIProviderType.ON_DEVICE_GENAI,
                profile.onDeviceGenAIAvailability
            )

            val (selectedType, _) = route()
            updateDiagnostics(selectedType, profile)
            profile
        } catch (t: Throwable) {
            logError("Capability detection failed", t)
            registry.updateAvailability(
                AIProviderType.ON_DEVICE_GENAI,
                AIAvailabilityStatus.ERROR
            )
            _diagnostics.update {
                it.copy(
                    detectedProvider = AIProviderType.ON_DEVICE_GENAI,
                    runtimeStatus = AIAvailabilityStatus.ERROR,
                    activeProvider = AIProviderType.DETERMINISTIC_LOCAL,
                    errorMessage = t.message ?: "Capability check failed",
                    isFallbackActive = true
                )
            }
            null
        }
    }

    private fun updateDiagnostics(
        activeProvider: AIProviderType,
        profile: DeviceAIProfile?
    ) {
        val onDeviceMeta = registry.getMetadata(AIProviderType.ON_DEVICE_GENAI)
        val genAiStatus = profile?.onDeviceGenAIAvailability
            ?: onDeviceMeta?.availabilityStatus
            ?: AIAvailabilityStatus.UNAVAILABLE

        val activeMeta = when (activeProvider) {
            AIProviderType.ON_DEVICE_GENAI -> onDeviceMeta
            AIProviderType.EMBEDDED_LOCAL_MODEL -> registry.getMetadata(AIProviderType.EMBEDDED_LOCAL_MODEL)
            else -> registry.getMetadata(AIProviderType.DETERMINISTIC_LOCAL)
        }

        val isOnline = try {
            networkProvider?.getCurrentState() != NetworkState.OFFLINE
        } catch (_: Throwable) {
            profile?.isNetworkAvailable ?: false
        }

        val embeddedDiag = embeddedModelManager?.diagnostics?.value ?: EmbeddedModelDiagnostics()
        val embeddedStatus = embeddedModelManager?.status?.value ?: EmbeddedModelStatus.NOT_INSTALLED

        _diagnostics.update {
            it.copy(
                detectedProvider = AIProviderType.ON_DEVICE_GENAI,
                runtimeStatus = genAiStatus,
                activeProvider = activeProvider,
                currentModel = activeMeta?.displayName ?: fallbackEngine.modelInfo.name,
                supportedCapabilities = activeMeta?.supportedCapabilities?.toList() ?: listOf(
                    AICapability.CHAT,
                    AICapability.STRUCTURED_OUTPUT,
                    AICapability.TEXT_GENERATION
                ),
                isFallbackActive = activeProvider == AIProviderType.DETERMINISTIC_LOCAL,
                embeddedModelStatus = embeddedStatus,
                embeddedModelDiagnostics = embeddedDiag,
                deviceApiLevel = profile?.apiLevel ?: try { android.os.Build.VERSION.SDK_INT } catch (_: Throwable) { 0 },
                deviceManufacturer = profile?.manufacturer ?: try { android.os.Build.MANUFACTURER ?: "Generic" } catch (_: Throwable) { "Generic" },
                deviceModel = profile?.model ?: try { android.os.Build.MODEL ?: "Device" } catch (_: Throwable) { "Device" },
                isNetworkAvailable = isOnline,
                isAiCoreInstalled = profile?.isAiCoreInstalled ?: false,
                lastCheckedTimestamp = System.currentTimeMillis(),
                errorMessage = null
            )
        }
    }

    // --- AIEngine implementation dynamically delegated to routed provider ---

    override val modelInfo: AIModelInfo
        get() = route().second.modelInfo

    override val isReady: Boolean
        get() = route().second.isReady

    override suspend fun classifyIntent(input: String): SummerIntent {
        val (_, engine) = route(AICapability.CHAT)
        val intent = engine.classifyIntent(input)
        return if (intent is SummerIntent.Unknown && engine != fallbackEngine) {
            // Safe fallback to deterministic parser
            fallbackEngine.classifyIntent(input)
        } else {
            intent
        }
    }

    override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult {
        val (providerType, engine) = route(AICapability.CHAT)
        logInfo("Processing interaction id=${interaction.id} with provider ${providerType.name}")
        val result = engine.process(context, interaction)

        // If advanced engine produced unknown or empty, guarantee fallback to deterministic core
        return if (result.intent is SummerIntent.Unknown && engine != fallbackEngine) {
            logInfo("Provider returned Unknown intent; falling back to deterministic local reasoning.")
            fallbackEngine.process(context, interaction)
        } else {
            result
        }
    }

    override suspend fun processQuery(request: AIRequest): AIResponse {
        val (_, engine) = route(AICapability.TEXT_GENERATION)
        return engine.processQuery(request)
    }

    override suspend fun evaluateIntent(input: String): RecognizedIntent {
        val (_, engine) = route(AICapability.CHAT)
        return engine.evaluateIntent(input)
    }

    private fun logInfo(msg: String) {
        try {
            android.util.Log.i(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun logError(msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(TAG, msg, tr)
        } catch (_: Throwable) {
            System.err.println("[$TAG] $msg: ${tr?.message}")
        }
    }
}

