package com.example.ai.localmodel

import com.example.ai.benchmark.ColdStartMetrics
import com.example.ai.benchmark.InferencePerformanceMetrics

/**
 * Result of direct, low-overhead inference execution against the embedded local model.
 * Bypasses orchestrators, decision engines, and deterministic fallbacks.
 */
data class DirectInferenceResult(
    val outputText: String,
    val timeToFirstResponseMs: Long,
    val totalInferenceTimeMs: Long,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val tokensPerSecond: Double? = null,
    val prefillTokensPerSecond: Double? = null,
    val initTimeInSecond: Double? = null,
    val rawBenchmarkInfoAvailable: Boolean = false
) {
    fun toPerformanceMetrics(): InferencePerformanceMetrics {
        return InferencePerformanceMetrics(
            timeToFirstResponseMs = timeToFirstResponseMs,
            totalInferenceTimeMs = totalInferenceTimeMs,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            tokensPerSecond = tokensPerSecond,
            prefillTokensPerSecond = prefillTokensPerSecond,
            initTimeInSecond = initTimeInSecond,
            tokenMetricsAvailable = rawBenchmarkInfoAvailable && (inputTokens != null || outputTokens != null || tokensPerSecond != null)
        )
    }
}

/**
 * Interface enabling direct benchmarking of the embedded generative model
 * without passing through unnecessary orchestration, memory, or speech pipelines.
 */
interface DirectInferenceEngine {
    val isModelInstalledAndReady: Boolean

    /**
     * Executes prompt inference directly on the underlying model engine.
     * Does NOT fall back to deterministic or cloud providers.
     */
    suspend fun executeDirectInference(
        prompt: String,
        systemInstruction: String? = null
    ): DirectInferenceResult

    /**
     * Measures cold start metrics (initialization + first inference).
     */
    suspend fun measureColdStart(samplePrompt: String = "What is the capital of Ghana?"): ColdStartMetrics

    /**
     * Returns runtime and environment diagnostics exposed by the model engine.
     */
    fun getRuntimeModelDiagnostics(): Map<String, String>
}
