package com.example.vision

import kotlinx.coroutines.flow.StateFlow

/**
 * Primary contract for the vision subsystem.
 * Provider-agnostic: exposes reactive state, diagnostics, and structured processing hooks.
 */
interface VisionEngine {
    val visionState: StateFlow<VisionState>
    val diagnostics: StateFlow<VisionDiagnostics>

    suspend fun process(input: VisionInput): VisionResult
    suspend fun stop()
    fun release()

    // Backward-compatible hooks
    val isReady: Boolean get() = visionState.value == VisionState.READY || visionState.value == VisionState.IDLE
    suspend fun processFrame(frameBytes: ByteArray): String = "Vision processing is not currently enabled."
}
