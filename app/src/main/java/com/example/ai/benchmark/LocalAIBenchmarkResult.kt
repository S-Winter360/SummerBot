package com.example.ai.benchmark

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Execution status of an individual benchmark test case.
 */
enum class CaseExecutionStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}

/**
 * Result of an individual benchmark test case.
 */
data class LocalAIBenchmarkCaseResult(
    val testCase: LocalAIBenchmarkCase,
    val status: CaseExecutionStatus,
    val outputText: String = "",
    val metrics: InferencePerformanceMetrics? = null,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = status == CaseExecutionStatus.SUCCESS
    val outputLength: Int get() = outputText.length
}

/**
 * Aggregate result of a complete benchmark suite run.
 */
data class LocalAIBenchmarkSuiteResult(
    val timestamp: Long = System.currentTimeMillis(),
    val systemInfo: DeviceSystemInfo,
    val modelInfo: Map<String, String>,
    val coldStartMetrics: ColdStartMetrics?,
    val caseResults: List<LocalAIBenchmarkCaseResult>,
    val averageWarmLatencyMs: Long,
    val averageTokensPerSecond: Double?,
    val diagnosticAssessment: String
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Generates a complete Markdown benchmark report suitable for copy-to-clipboard
     * and external physical device diagnostic review.
     */
    fun toMarkdownReport(): String {
        val sb = StringBuilder()
        sb.appendLine("# Summer Winter — Embedded Local AI Benchmark Report")
        sb.appendLine("Date: ${formattedDate()}")
        sb.appendLine()

        sb.appendLine("## 1. System & Hardware Environment")
        sb.appendLine("- Device: ${systemInfo.manufacturer} ${systemInfo.deviceModel}")
        sb.appendLine("- Android Version: Android ${systemInfo.androidVersion} (API ${systemInfo.apiLevel})")
        sb.appendLine("- Processors: ${systemInfo.availableProcessors} cores")
        sb.appendLine("- Memory: ${systemInfo.availableMemoryMb} MB free / ${systemInfo.totalMemoryMb} MB total (Low memory: ${systemInfo.isLowMemory})")
        sb.appendLine("- Configured Backend: ${systemInfo.runtimeBackend}")
        sb.appendLine()

        sb.appendLine("## 2. Model Information")
        for ((k, v) in modelInfo) {
            sb.appendLine("- $k: $v")
        }
        sb.appendLine()

        sb.appendLine("## 3. Cold Start Metrics")
        if (coldStartMetrics != null) {
            sb.appendLine("- Model Initialization Time: ${coldStartMetrics.modelInitializationTimeMs} ms")
            sb.appendLine("- First Inference Time: ${coldStartMetrics.firstInferenceTimeMs} ms")
            sb.appendLine("- Total Cold Start Time: ${coldStartMetrics.totalColdStartTimeMs} ms")
            if (coldStartMetrics.wasAlreadyInitialized) {
                sb.appendLine("- Note: Engine was already resident in memory during this run")
            }
        } else {
            sb.appendLine("- Cold start was not measured.")
        }
        sb.appendLine()

        sb.appendLine("## 4. Controlled Test Cases (Warm Inference)")
        for (res in caseResults) {
            sb.appendLine("### ${res.testCase.title}")
            sb.appendLine("- Status: ${res.status.name}")
            sb.appendLine("- Prompt: \"${res.testCase.prompt}\"")
            sb.appendLine("- Expected: ${res.testCase.expectedCapability}")
            if (res.metrics != null) {
                sb.appendLine("- Time to First Response (TTFT): ${res.metrics.timeToFirstResponseMs} ms")
                sb.appendLine("- Total Latency: ${res.metrics.totalInferenceTimeMs} ms")
                sb.appendLine("- Token Metrics: ${res.metrics.tokenMetricsSummary()}")
            }
            if (res.isSuccess) {
                sb.appendLine("- Output Length: ${res.outputLength} characters")
                sb.appendLine("- Output:")
                sb.appendLine("```")
                sb.appendLine(res.outputText)
                sb.appendLine("```")
            } else if (res.errorMessage != null) {
                sb.appendLine("- Error: ${res.errorMessage}")
            }
            sb.appendLine()
        }

        sb.appendLine("## 5. Summary & Diagnostic Assessment")
        sb.appendLine("- Average Warm Latency: $averageWarmLatencyMs ms")
        if (averageTokensPerSecond != null && averageTokensPerSecond > 0.0) {
            sb.appendLine("- Average Tokens/Second: ${String.format(Locale.US, "%.1f", averageTokensPerSecond)} tok/s")
        } else {
            sb.appendLine("- Average Tokens/Second: Token metrics unavailable")
        }
        sb.appendLine()
        sb.appendLine("### Engineering Diagnostic Findings:")
        sb.appendLine(diagnosticAssessment)

        return sb.toString()
    }
}
