package com.example.ai.benchmark

import java.util.Locale

/**
 * Hardware and runtime environment details captured during benchmark initialization.
 */
data class DeviceSystemInfo(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val availableProcessors: Int,
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val isLowMemory: Boolean,
    val runtimeBackend: String
) {
    fun summary(): String {
        return "$manufacturer $deviceModel (Android $androidVersion, API $apiLevel) | $availableProcessors cores | $availableMemoryMb MB free / $totalMemoryMb MB RAM | Backend: $runtimeBackend"
    }
}

/**
 * Latency metrics capturing model cold initialization and first inference.
 */
data class ColdStartMetrics(
    val modelInitializationTimeMs: Long,
    val firstInferenceTimeMs: Long,
    val totalColdStartTimeMs: Long,
    val wasAlreadyInitialized: Boolean = false
) {
    fun summary(): String {
        return "Init: ${modelInitializationTimeMs}ms, 1st Inference: ${firstInferenceTimeMs}ms, Total Cold Start: ${totalColdStartTimeMs}ms" +
                if (wasAlreadyInitialized) " (engine was already active)" else ""
    }
}

/**
 * Detailed inference latency and throughput metrics for an individual benchmark run.
 */
data class InferencePerformanceMetrics(
    val timeToFirstResponseMs: Long,
    val totalInferenceTimeMs: Long,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val tokensPerSecond: Double? = null,
    val prefillTokensPerSecond: Double? = null,
    val initTimeInSecond: Double? = null,
    val tokenMetricsAvailable: Boolean = false
) {
    /**
     * Human-readable token performance summary complying with benchmark rules:
     * Shows actual tokens per second if exposed by runtime, or "Token metrics unavailable".
     */
    fun tokenMetricsSummary(): String {
        return if (tokenMetricsAvailable && tokensPerSecond != null && tokensPerSecond > 0.0) {
            val decTps = String.format(Locale.US, "%.1f", tokensPerSecond)
            val inTok = inputTokens?.toString() ?: "?"
            val outTok = outputTokens?.toString() ?: "?"
            "$decTps tok/s (in: $inTok, out: $outTok)"
        } else {
            "Token metrics unavailable"
        }
    }

    fun latencySummary(): String {
        return "TTFT: ${timeToFirstResponseMs}ms | Total: ${totalInferenceTimeMs}ms"
    }
}
