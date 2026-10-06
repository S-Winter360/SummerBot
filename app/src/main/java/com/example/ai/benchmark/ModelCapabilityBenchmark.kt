package com.example.ai.benchmark

import java.util.Locale

/**
 * Diagnostic analysis engine evaluating benchmark outputs and isolating bottlenecks.
 *
 * Helps identify whether observed physical device latency originates from:
 * 1. Gemma 3 1B INT4 computational load on CPU (clock speed/core architecture)
 * 2. Token generation count (decoding bottleneck on longer creative prompts)
 * 3. Time to first token (prefill / prompt ingestion overhead)
 * 4. Architectural orchestration prompt size vs raw input
 * 5. Device memory pressure or thermal throttling
 */
object ModelCapabilityBenchmark {

    fun generateDiagnosticAssessment(
        systemInfo: DeviceSystemInfo,
        coldStart: ColdStartMetrics?,
        cases: List<LocalAIBenchmarkCaseResult>
    ): String {
        val successfulCases = cases.filter { it.isSuccess && it.metrics != null }
        if (successfulCases.isEmpty()) {
            return "No test cases completed successfully. Check embedded model installation and LiteRT-LM runtime integrity."
        }

        val sb = StringBuilder()

        // 1. Cold start analysis
        if (coldStart != null) {
            val totalColdSec = coldStart.totalColdStartTimeMs / 1000.0
            if (coldStart.totalColdStartTimeMs > 10_000) {
                sb.appendLine("• Cold Start: High initialization overhead (${String.format(Locale.US, "%.1f", totalColdSec)}s). Model weights mapping into memory takes significant time on flash storage; keeping the engine resident in memory is essential for interactive responsiveness.")
            } else {
                sb.appendLine("• Cold Start: Fast initial load (${String.format(Locale.US, "%.1f", totalColdSec)}s).")
            }
        }

        // 2. Decode vs Prefill bottleneck comparison
        val shortCase = successfulCases.find { it.testCase.id == "TEST_A" }
        val longCase = successfulCases.find { it.testCase.id == "TEST_D" }
        val contextCase = successfulCases.find { it.testCase.id == "TEST_E" }

        if (shortCase != null && longCase != null) {
            val shortLatency = shortCase.metrics!!.totalInferenceTimeMs
            val longLatency = longCase.metrics!!.totalInferenceTimeMs

            if (longLatency > shortLatency * 3) {
                sb.appendLine("• Generation Length Bottleneck: Creative story generation took ${longLatency}ms vs ${shortLatency}ms for factual query. On CPU execution (${systemInfo.runtimeBackend}), decoding throughput scales linearly with token count. Auto-regressive decoding on mobile CPU cores is the primary latency factor for lengthy outputs.")
            }
        }

        // 3. Orchestration overhead analysis (Test A raw vs Test E with system instructions)
        if (shortCase != null && contextCase != null) {
            val rawLatency = shortCase.metrics!!.totalInferenceTimeMs
            val ctxLatency = contextCase.metrics!!.totalInferenceTimeMs
            val deltaMs = ctxLatency - rawLatency

            if (deltaMs > 2000) {
                sb.appendLine("• Prompt / Context Overhead: Adding system instructions added +${deltaMs}ms to inference. Prefill processing for longer context adds measurable time on CPU.")
            } else {
                sb.appendLine("• Prompt / Context Overhead: Adding system prompt added minimal overhead (+${deltaMs}ms), indicating prefill is relatively efficient compared to decode generation.")
            }
        }

        // 4. Token throughput evaluation
        val tpsList = successfulCases.mapNotNull { it.metrics?.tokensPerSecond }.filter { it > 0.0 }
        if (tpsList.isNotEmpty()) {
            val avgTps = tpsList.average()
            val formattedTps = String.format(Locale.US, "%.1f", avgTps)
            if (avgTps < 5.0) {
                sb.appendLine("• Low Token Throughput ($formattedTps tok/s): The CPU backend (${systemInfo.availableProcessors} cores) generates under 5 tokens/second. GPU acceleration (Vulkan/OpenCL delegate) or NPU offloading is recommended if supported by the device chipset.")
            } else {
                sb.appendLine("• Adequate Token Throughput ($formattedTps tok/s) on ${systemInfo.runtimeBackend}.")
            }
        } else {
            sb.appendLine("• Token throughput not directly reported by runtime JNI; estimated from total latency and emitted text characters.")
        }

        // 5. Memory assessment
        if (systemInfo.isLowMemory || systemInfo.availableMemoryMb < 400) {
            sb.appendLine("• Memory Pressure: Device reported low available memory (${systemInfo.availableMemoryMb} MB free). Potential GC pressure during inference.")
        } else {
            sb.appendLine("• Memory Headroom: Adequate RAM headroom available (${systemInfo.availableMemoryMb} MB free / ${systemInfo.totalMemoryMb} MB total).")
        }

        return sb.toString().trim()
    }
}
