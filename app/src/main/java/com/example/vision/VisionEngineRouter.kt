package com.example.vision

import com.example.core.event.EventPriority
import com.example.core.event.SummerEvent
import com.example.core.event.SummerEventBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Provider-selection and lifecycle-coordination router for the vision subsystem.
 * Mediates requests between Summer components and underlying vision providers.
 */
class VisionEngineRouter(
    val stubProvider: VisionProvider = StubVisionProvider(),
    private val detector: VisionCapabilityDetector? = null,
    private val eventBus: SummerEventBus? = null,
    private val routerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : VisionEngine {

    private val _visionState = MutableStateFlow(VisionState.IDLE)
    override val visionState: StateFlow<VisionState> = _visionState.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        detector?.detectDiagnostics(
            activeProvider = stubProvider.providerType,
            visionState = VisionState.IDLE
        ) ?: VisionDiagnostics(
            activeProviderType = stubProvider.providerType,
            visionState = VisionState.IDLE
        )
    )
    override val diagnostics: StateFlow<VisionDiagnostics> = _diagnostics.asStateFlow()

    private var activeJob: Job? = null

    init {
        refreshDiagnostics()
    }

    fun getActiveProvider(): VisionProvider? {
        return if (stubProvider.isAvailable) stubProvider else null
    }

    fun refreshDiagnostics() {
        val currentProviderType = getActiveProvider()?.providerType ?: VisionProviderType.STUB
        if (detector != null) {
            _diagnostics.value = detector.detectDiagnostics(
                activeProvider = currentProviderType,
                visionState = _visionState.value
            )
        } else {
            _diagnostics.update {
                it.copy(
                    activeProviderType = currentProviderType,
                    visionState = _visionState.value,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    override suspend fun process(input: VisionInput): VisionResult = withContext(Dispatchers.Default) {
        // Enforce permission validation if input originates from CAMERA
        if (input.source == VisionInputSource.CAMERA && detector != null) {
            val permission = detector.getCameraPermissionState()
            if (permission == CameraPermissionState.DENIED || permission == CameraPermissionState.PERMANENTLY_DENIED) {
                _visionState.value = VisionState.ERROR
                refreshDiagnostics()
                return@withContext VisionResult.PermissionDenied(
                    "Camera permission is not granted (Status: $permission)."
                )
            }
        }

        val provider = getActiveProvider()
        if (provider == null) {
            _visionState.value = VisionState.ERROR
            refreshDiagnostics()
            return@withContext VisionResult.Unavailable("No vision provider is currently available.")
        }

        _visionState.value = VisionState.PROCESSING
        refreshDiagnostics()

        try {
            val result = provider.process(input)
            when (result) {
                is VisionResult.Success -> {
                    _visionState.value = VisionState.COMPLETED
                    // Post structured event to SummerEventBus
                    eventBus?.publish(
                        SummerEvent.VisionInput(
                            frameSummary = result.observation.toContextSummary(),
                            priority = EventPriority.LOW
                        )
                    )
                }
                is VisionResult.NoObservation -> {
                    _visionState.value = VisionState.COMPLETED
                }
                is VisionResult.PermissionDenied -> {
                    _visionState.value = VisionState.ERROR
                }
                is VisionResult.Unavailable,
                is VisionResult.Unsupported,
                is VisionResult.Error -> {
                    _visionState.value = VisionState.ERROR
                }
            }
            refreshDiagnostics()
            result
        } catch (c: CancellationException) {
            _visionState.value = VisionState.STOPPED
            refreshDiagnostics()
            VisionResult.Error("Vision processing was cancelled.")
        } catch (t: Throwable) {
            _visionState.value = VisionState.ERROR
            refreshDiagnostics()
            VisionResult.Error(
                message = t.message ?: "An unexpected error occurred during vision processing.",
                throwable = t
            )
        }
    }

    override suspend fun stop() {
        activeJob?.cancel()
        activeJob = null
        stubProvider.stop()
        _visionState.value = VisionState.STOPPED
        refreshDiagnostics()
    }

    override fun release() {
        activeJob?.cancel()
        activeJob = null
        stubProvider.release()
        _visionState.value = VisionState.IDLE
        refreshDiagnostics()
    }
}
