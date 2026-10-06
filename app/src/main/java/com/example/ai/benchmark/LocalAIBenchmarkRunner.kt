package com.example.ai.benchmark

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.ai.localmodel.DirectInferenceEngine
import com.example.ai.localmodel.EmbeddedModelManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State of benchmark execution for UI observation.
 */
sealed class BenchmarkRunState {
    data class Idle(val lastResult: LocalAIBenchmarkSuiteResult? = null) : BenchmarkRunState()
    data class Running(
        val currentCaseIndex: Int,
        val totalCases: Int,
        val currentCaseTitle: String,
        val partialResults: List<LocalAIBenchmarkCaseResult>
    ) : BenchmarkRunState()
    data class Completed(val result: LocalAIBenchmarkSuiteResult) : BenchmarkRunState()
    data class Error(val message: String, val lastResult: LocalAIBenchmarkSuiteResult? = null) : BenchmarkRunState()
}

/**
 * Executes controlled developer benchmarks directly against the embedded LiteRT-LM engine.
 *
 * Strictly adheres to architectural rules:
 * - Direct path: Benchmark -> EmbeddedLocalAIEngine -> LiteRT-LM -> Gemma 3 1B IT INT4.
 * - Bypasses SummerOrchestrator, DecisionEngine, MainViewModel, Voice, and Fallback.
 * - Never silently switches to Gemini Nano, Deterministic Fallback, or Cloud AI.
 */
class LocalAIBenchmarkRunner(
    private val context: Context,
    private val directEngine: DirectInferenceEngine,
    private val modelManager: EmbeddedModelManager
) {
    companion object {
        private const val TAG = "LocalAIBenchmarkRunner"
    }

    private val _state = MutableStateFlow<BenchmarkRunState>(BenchmarkRunState.Idle())
    val state: StateFlow<BenchmarkRunState> = _state.asStateFlow()

    private var benchmarkJob: Job? = null

    /**
     * Inspects safe device and hardware environment properties.
     */
    fun collectSystemInfo(): DeviceSystemInfo {
        var totalMemMb = 0L
        var availMemMb = 0L
        var isLowMem = false

        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                totalMemMb = memInfo.totalMem / (1024 * 1024)
                availMemMb = memInfo.availMem / (1024 * 1024)
                isLowMem = memInfo.lowMemory
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read ActivityManager MemoryInfo: ${t.message}")
        }

        val engineDiag = directEngine.getRuntimeModelDiagnostics()
        val backend = engineDiag["Backend"] ?: "CPU (Configured)"

        return DeviceSystemInfo(
            deviceModel = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            availableProcessors = Runtime.getRuntime().availableProcessors(),
            totalMemoryMb = totalMemMb,
            availableMemoryMb = availMemMb,
            isLowMemory = isLowMem,
            runtimeBackend = backend
        )
    }

    /**
     * Inspects current model metadata safely.
     */
    fun collectModelInfo(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val engineDiag = directEngine.getRuntimeModelDiagnostics()

        map["Model"] = engineDiag["Model"] ?: "Gemma 3 1B IT INT4"
        map["Model ID"] = modelManager.diagnostics.value.modelId.name
        map["Runtime"] = engineDiag["Runtime"] ?: "LiteRT-LM"
        map["Quantization"] = engineDiag["Quantization"] ?: "INT4"
        map["Model Status"] = modelManager.status.value.name
        map["Configured Backend"] = engineDiag["Backend"] ?: "CPU (Configured)"
        map["Execution Path"] = "Direct LiteRT-LM (Zero-Orchestrator Bypass)"

        return map
    }

    /**
     * Launches the full benchmark suite.
     */
    fun runFullBenchmark(scope: CoroutineScope): Job {
        if (_state.value is BenchmarkRunState.Running) {
            Log.w(TAG, "Benchmark already running; ignoring duplicate request.")
            return benchmarkJob ?: Job().apply { complete() }
        }

        benchmarkJob?.cancel()
        val job = scope.launch(Dispatchers.Default) {
            executeBenchmarkSuite()
        }
        benchmarkJob = job
        return job
    }

    /**
     * Suspending version executing the suite synchronously in the calling coroutine.
     */
    suspend fun runBenchmarkSuspending() {
        executeBenchmarkSuite()
    }

    /**
     * Cancels any currently executing benchmark.
     */
    fun cancel() {
        benchmarkJob?.cancel()
        benchmarkJob = null
        val current = _state.value
        val lastRes = when (current) {
            is BenchmarkRunState.Idle -> current.lastResult
            is BenchmarkRunState.Completed -> current.result
            is BenchmarkRunState.Error -> current.lastResult
            is BenchmarkRunState.Running -> null
        }
        _state.value = BenchmarkRunState.Idle(lastRes)
    }

    /**
     * Resets state back to Idle without prior results.
     */
    fun reset() {
        cancel()
        _state.value = BenchmarkRunState.Idle(null)
    }

    private suspend fun executeBenchmarkSuite() = withContext(Dispatchers.Default) {
        val systemInfo = collectSystemInfo()
        val modelInfo = collectModelInfo()

        // 1. Strict validation: Is the embedded model actually installed and ready?
        if (!directEngine.isModelInstalledAndReady || !modelManager.isModelReady()) {
            val errorMsg = "Cannot benchmark: Embedded model is not ready (Status: ${modelManager.status.value.name}). Import a valid .litertlm model in Settings."
            Log.e(TAG, errorMsg)
            _state.value = BenchmarkRunState.Error(errorMsg)
            return@withContext
        }

        val cases = LocalAIBenchmarkCase.SUITE
        val caseResults = mutableListOf<LocalAIBenchmarkCaseResult>()

        _state.value = BenchmarkRunState.Running(
            currentCaseIndex = 0,
            totalCases = cases.size + 1, // +1 for cold start
            currentCaseTitle = "Cold Start Measurement",
            partialResults = emptyList()
        )

        // 2. Measure Cold Start
        var coldStartMetrics: ColdStartMetrics? = null
        try {
            coldStartMetrics = directEngine.measureColdStart()
            Log.i(TAG, "Cold start measured: ${coldStartMetrics.summary()}")
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "Cold start measurement failed: ${t.message}")
        }

        // 3. Execute Controlled Warm Benchmark Cases
        for ((idx, testCase) in cases.withIndex()) {
            _state.value = BenchmarkRunState.Running(
                currentCaseIndex = idx + 1,
                totalCases = cases.size + 1,
                currentCaseTitle = testCase.title,
                partialResults = caseResults.toList()
            )

            try {
                Log.i(TAG, "Running benchmark case: ${testCase.title}")
                val directResult = directEngine.executeDirectInference(
                    prompt = testCase.prompt,
                    systemInstruction = testCase.systemInstruction
                )

                val perfMetrics = directResult.toPerformanceMetrics()
                val result = LocalAIBenchmarkCaseResult(
                    testCase = testCase,
                    status = CaseExecutionStatus.SUCCESS,
                    outputText = directResult.outputText,
                    metrics = perfMetrics
                )
                caseResults.add(result)
                Log.i(TAG, "Completed case ${testCase.id}: ${perfMetrics.latencySummary()} | ${perfMetrics.tokenMetricsSummary()}")
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.e(TAG, "Failed case ${testCase.id}: ${t.message}", t)
                val failedResult = LocalAIBenchmarkCaseResult(
                    testCase = testCase,
                    status = CaseExecutionStatus.FAILED,
                    errorMessage = t.message ?: "Unknown execution error"
                )
                caseResults.add(failedResult)
            }
        }

        // 4. Compute Aggregate Averages & Diagnostic Findings
        val completedMetrics = caseResults.mapNotNull { it.metrics }
        val avgLatency = if (completedMetrics.isNotEmpty()) {
            completedMetrics.map { it.totalInferenceTimeMs }.average().toLong()
        } else {
            0L
        }

        val availableTps = completedMetrics.mapNotNull { it.tokensPerSecond }.filter { it > 0.0 }
        val avgTps = if (availableTps.isNotEmpty()) availableTps.average() else null

        val diagnosticFindings = ModelCapabilityBenchmark.generateDiagnosticAssessment(
            systemInfo = systemInfo,
            coldStart = coldStartMetrics,
            cases = caseResults
        )

        val suiteResult = LocalAIBenchmarkSuiteResult(
            timestamp = System.currentTimeMillis(),
            systemInfo = systemInfo,
            modelInfo = modelInfo,
            coldStartMetrics = coldStartMetrics,
            caseResults = caseResults,
            averageWarmLatencyMs = avgLatency,
            averageTokensPerSecond = avgTps,
            diagnosticAssessment = diagnosticFindings
        )

        _state.value = BenchmarkRunState.Completed(suiteResult)
        Log.i(TAG, "Embedded Local AI Benchmark complete. Avg warm latency: ${avgLatency}ms")
    }
}
