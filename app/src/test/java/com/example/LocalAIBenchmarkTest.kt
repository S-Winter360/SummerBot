package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.benchmark.BenchmarkCategory
import com.example.ai.benchmark.BenchmarkRunState
import com.example.ai.benchmark.CaseExecutionStatus
import com.example.ai.benchmark.ColdStartMetrics
import com.example.ai.benchmark.DeviceSystemInfo
import com.example.ai.benchmark.InferencePerformanceMetrics
import com.example.ai.benchmark.LocalAIBenchmarkCase
import com.example.ai.benchmark.LocalAIBenchmarkCaseResult
import com.example.ai.benchmark.LocalAIBenchmarkRunner
import com.example.ai.benchmark.LocalAIBenchmarkSuiteResult
import com.example.ai.benchmark.ModelCapabilityBenchmark
import com.example.ai.localmodel.DirectInferenceEngine
import com.example.ai.localmodel.DirectInferenceResult
import com.example.ai.localmodel.EmbeddedModelManager
import com.example.ai.localmodel.EmbeddedModelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalAIBenchmarkTest {

    private class FakeDirectInferenceEngine(
        override var isModelInstalledAndReady: Boolean = true,
        var throwOnError: Boolean = false,
        var mockTokensPerSecond: Double? = 8.5
    ) : DirectInferenceEngine {
        val executedPrompts = mutableListOf<String>()
        var coldStartMeasured = false

        override suspend fun executeDirectInference(
            prompt: String,
            systemInstruction: String?
        ): DirectInferenceResult {
            if (throwOnError) {
                throw IllegalStateException("Simulated engine inference failure")
            }
            executedPrompts.add(prompt)
            return DirectInferenceResult(
                outputText = "Generated response for: $prompt",
                timeToFirstResponseMs = 450L,
                totalInferenceTimeMs = 1200L,
                inputTokens = 12,
                outputTokens = 24,
                tokensPerSecond = mockTokensPerSecond,
                prefillTokensPerSecond = 45.0,
                initTimeInSecond = 0.2,
                rawBenchmarkInfoAvailable = mockTokensPerSecond != null
            )
        }

        override suspend fun measureColdStart(samplePrompt: String): ColdStartMetrics {
            coldStartMeasured = true
            return ColdStartMetrics(
                modelInitializationTimeMs = 1800L,
                firstInferenceTimeMs = 850L,
                totalColdStartTimeMs = 2650L,
                wasAlreadyInitialized = false
            )
        }

        override fun getRuntimeModelDiagnostics(): Map<String, String> {
            return mapOf(
                "Model" to "Gemma 3 1B IT INT4",
                "Runtime" to "LiteRT-LM",
                "Quantization" to "INT4",
                "Backend" to "CPU (Configured)"
            )
        }
    }

    private class FakeEmbeddedModelManager(
        var ready: Boolean = true
    ) : EmbeddedModelManager {
        private val _status = MutableStateFlow(if (ready) EmbeddedModelStatus.READY else EmbeddedModelStatus.NOT_INSTALLED)
        override val status: StateFlow<EmbeddedModelStatus> = _status
        private val _downloadProgress = MutableStateFlow(0f)
        override val downloadProgress: StateFlow<Float> = _downloadProgress
        private val _diagnostics = MutableStateFlow(
            com.example.ai.localmodel.EmbeddedModelDiagnostics(
                modelId = com.example.ai.localmodel.EmbeddedModelId.GEMMA_3_1B_IT,
                status = if (ready) EmbeddedModelStatus.READY else EmbeddedModelStatus.NOT_INSTALLED
            )
        )
        override val diagnostics: StateFlow<com.example.ai.localmodel.EmbeddedModelDiagnostics> = _diagnostics

        override fun isModelReady(): Boolean = ready
        override fun getInstalledModelPath(): String? = if (ready) "/fake/model.litertlm" else null
        override suspend fun verify(): Result<Unit> = Result.success(Unit)
        override suspend fun inspect(): EmbeddedModelStatus = _status.value
        override suspend fun importModel(uri: android.net.Uri): Result<Unit> = Result.success(Unit)
        override suspend fun importModelStream(
            inputStreamProvider: () -> java.io.InputStream?,
            fileNameHint: String?,
            totalSizeBytes: Long?
        ): Result<Unit> = Result.success(Unit)
        override suspend fun cancelImport() {}
        override suspend fun deleteInstalledModel(): Result<Unit> {
            ready = false
            _status.value = EmbeddedModelStatus.NOT_INSTALLED
            return Result.success(Unit)
        }
    }

    @Test
    fun testCollectSystemAndModelInfo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = FakeDirectInferenceEngine()
        val manager = FakeEmbeddedModelManager()
        val runner = LocalAIBenchmarkRunner(context, engine, manager)

        val sysInfo = runner.collectSystemInfo()
        assertNotNull(sysInfo.deviceModel)
        assertTrue(sysInfo.availableProcessors > 0)
        assertEquals("CPU (Configured)", sysInfo.runtimeBackend)

        val modelInfo = runner.collectModelInfo()
        assertEquals("Gemma 3 1B IT INT4", modelInfo["Model"])
        assertEquals("GEMMA_3_1B_IT", modelInfo["Model ID"])
        assertEquals("LiteRT-LM", modelInfo["Runtime"])
        assertEquals("INT4", modelInfo["Quantization"])
        assertEquals("READY", modelInfo["Model Status"])
    }

    @Test
    fun testTokenMetricsFormattingWhenAvailable() {
        val metrics = InferencePerformanceMetrics(
            timeToFirstResponseMs = 300L,
            totalInferenceTimeMs = 1500L,
            inputTokens = 15,
            outputTokens = 30,
            tokensPerSecond = 12.5,
            tokenMetricsAvailable = true
        )
        val summary = metrics.tokenMetricsSummary()
        assertTrue(summary.contains("12.5 tok/s"))
        assertTrue(summary.contains("in: 15"))
        assertTrue(summary.contains("out: 30"))
    }

    @Test
    fun testTokenMetricsFormattingWhenUnavailable() {
        val metrics = InferencePerformanceMetrics(
            timeToFirstResponseMs = 300L,
            totalInferenceTimeMs = 1500L,
            inputTokens = null,
            outputTokens = null,
            tokensPerSecond = null,
            tokenMetricsAvailable = false
        )
        val summary = metrics.tokenMetricsSummary()
        assertEquals("Token metrics unavailable", summary)
    }

    @Test
    fun testBenchmarkFailsGracefullyWhenModelNotInstalled() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = FakeDirectInferenceEngine(isModelInstalledAndReady = false)
        val manager = FakeEmbeddedModelManager(ready = false)
        val runner = LocalAIBenchmarkRunner(context, engine, manager)

        runner.runBenchmarkSuspending()

        val state = runner.state.value
        assertTrue(state is BenchmarkRunState.Error)
        val errorState = state as BenchmarkRunState.Error
        assertTrue(errorMsgContainsReady(errorState.message))
        assertEquals(0, engine.executedPrompts.size)
    }

    private fun errorMsgContainsReady(msg: String): Boolean {
        return msg.contains("not ready", ignoreCase = true)
    }

    @Test
    fun testBenchmarkExecutesAllPrescribedCasesDirectly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = FakeDirectInferenceEngine()
        val manager = FakeEmbeddedModelManager(ready = true)
        val runner = LocalAIBenchmarkRunner(context, engine, manager)

        runner.runBenchmarkSuspending()

        val state = runner.state.value
        assertTrue(state is BenchmarkRunState.Completed)
        val completed = state as BenchmarkRunState.Completed

        assertTrue(engine.coldStartMeasured)
        // Cases in suite: Test A, Test B, Test C, Test D, Test E = 5
        assertEquals(5, completed.result.caseResults.size)
        assertEquals(5, engine.executedPrompts.size)

        // Verify prompts were executed directly
        assertTrue(engine.executedPrompts.contains("What is the capital of Ghana?"))
        assertTrue(engine.executedPrompts.contains("I'm tired today."))
        assertTrue(engine.executedPrompts.contains("Explain what Python programming is in simple terms."))
        assertTrue(engine.executedPrompts.contains("Tell me a short story about a young boy in Ghana who discovers something unexpected while walking home from school."))

        for (caseRes in completed.result.caseResults) {
            assertTrue(caseRes.isSuccess)
            assertNotNull(caseRes.metrics)
            assertTrue(caseRes.outputLength > 0)
        }

        assertTrue(completed.result.averageWarmLatencyMs > 0)
        assertNotNull(completed.result.averageTokensPerSecond)
    }

    @Test
    fun testMarkdownReportGeneration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = FakeDirectInferenceEngine()
        val manager = FakeEmbeddedModelManager(ready = true)
        val runner = LocalAIBenchmarkRunner(context, engine, manager)

        runner.runBenchmarkSuspending()

        val completed = runner.state.value as BenchmarkRunState.Completed
        val markdown = completed.result.toMarkdownReport()

        assertTrue(markdown.contains("# Summer Winter — Embedded Local AI Benchmark Report"))
        assertTrue(markdown.contains("## 1. System & Hardware Environment"))
        assertTrue(markdown.contains("## 2. Model Information"))
        assertTrue(markdown.contains("## 3. Cold Start Metrics"))
        assertTrue(markdown.contains("## 4. Controlled Test Cases"))
        assertTrue(markdown.contains("Test A — Simple Factual Question"))
        assertTrue(markdown.contains("Test D — Creative Generation"))
        assertTrue(markdown.contains("Engineering Diagnostic Findings"))
    }

    @Test
    fun testDiagnosticAssessmentBottleneckDetection() {
        val sysInfo = DeviceSystemInfo(
            deviceModel = "WP60",
            manufacturer = "OUKITEL",
            androidVersion = "14",
            apiLevel = 34,
            availableProcessors = 8,
            totalMemoryMb = 6000L,
            availableMemoryMb = 3200L,
            isLowMemory = false,
            runtimeBackend = "CPU (Configured)"
        )
        val coldStart = ColdStartMetrics(
            modelInitializationTimeMs = 12000L,
            firstInferenceTimeMs = 2500L,
            totalColdStartTimeMs = 14500L
        )
        val cases = listOf(
            LocalAIBenchmarkCaseResult(
                testCase = LocalAIBenchmarkCase.SUITE[0], // Test A
                status = CaseExecutionStatus.SUCCESS,
                outputText = "Accra is the capital of Ghana.",
                metrics = InferencePerformanceMetrics(
                    timeToFirstResponseMs = 800L,
                    totalInferenceTimeMs = 1800L,
                    tokensPerSecond = 3.2,
                    tokenMetricsAvailable = true
                )
            ),
            LocalAIBenchmarkCaseResult(
                testCase = LocalAIBenchmarkCase.SUITE[3], // Test D
                status = CaseExecutionStatus.SUCCESS,
                outputText = "Once upon a time in Ghana...",
                metrics = InferencePerformanceMetrics(
                    timeToFirstResponseMs = 1200L,
                    totalInferenceTimeMs = 9500L,
                    tokensPerSecond = 3.0,
                    tokenMetricsAvailable = true
                )
            )
        )

        val assessment = ModelCapabilityBenchmark.generateDiagnosticAssessment(sysInfo, coldStart, cases)
        assertTrue(assessment.contains("Cold Start: High initialization overhead"))
        assertTrue(assessment.contains("Generation Length Bottleneck"))
        assertTrue(assessment.contains("Low Token Throughput"))
    }
}
