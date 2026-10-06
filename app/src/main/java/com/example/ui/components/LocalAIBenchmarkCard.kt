package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.benchmark.BenchmarkRunState
import com.example.ai.benchmark.CaseExecutionStatus
import com.example.ai.benchmark.LocalAIBenchmarkCaseResult
import com.example.ai.benchmark.LocalAIBenchmarkRunner
import com.example.ai.benchmark.LocalAIBenchmarkSuiteResult
import com.example.ui.theme.CoreBlack
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CoreCharcoalSurface
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateBright
import com.example.ui.theme.SlateLight
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun LocalAIBenchmarkCard(
    benchmarkRunner: LocalAIBenchmarkRunner,
    modifier: Modifier = Modifier,
    onRunBenchmark: () -> Unit = {},
    onCancelBenchmark: () -> Unit = {},
    onResetBenchmark: () -> Unit = {}
) {
    val context = LocalContext.current
    val benchmarkState by benchmarkRunner.state.collectAsState()
    val systemInfo = remember(benchmarkRunner) { benchmarkRunner.collectSystemInfo() }
    val modelInfo = remember(benchmarkRunner) { benchmarkRunner.collectModelInfo() }

    val activeResult: LocalAIBenchmarkSuiteResult? = when (val state = benchmarkState) {
        is BenchmarkRunState.Completed -> state.result
        is BenchmarkRunState.Idle -> state.lastResult
        is BenchmarkRunState.Error -> state.lastResult
        is BenchmarkRunState.Running -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CoreCharcoalSurface)
            .border(1.dp, CoreCharcoalBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("embedded_ai_benchmark_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyanLuminous.copy(alpha = 0.15f))
                        .border(1.dp, CyanLuminous.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Benchmark icon",
                        tint = CyanLuminous,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Embedded AI Benchmark",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Direct LiteRT-LM inference diagnostics",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateLight
                    )
                }
            }
        }

        // 2. Hardware & Runtime Diagnostics Overview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CoreCharcoalElevated)
                .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BenchmarkInfoRow(label = "Target Device", value = "${systemInfo.manufacturer} ${systemInfo.deviceModel}")
            BenchmarkInfoRow(label = "OS Version", value = "Android ${systemInfo.androidVersion} (API ${systemInfo.apiLevel})")
            BenchmarkInfoRow(label = "Hardware Cores", value = "${systemInfo.availableProcessors} CPU cores")
            BenchmarkInfoRow(
                label = "RAM Capacity",
                value = "${systemInfo.availableMemoryMb} MB free / ${systemInfo.totalMemoryMb} MB"
            )
            BenchmarkInfoRow(label = "Active Backend", value = systemInfo.runtimeBackend)
            BenchmarkInfoRow(label = "Model ID", value = modelInfo["Model ID"] ?: "gemma-3-1b-it-litertlm")
            BenchmarkInfoRow(label = "Model Status", value = modelInfo["Model Status"] ?: "UNKNOWN")
            BenchmarkInfoRow(label = "Execution Boundary", value = "Direct LiteRT-LM (Bypasses Orchestrator & Fallback)")
        }

        // 3. Execution / Progress State
        when (val state = benchmarkState) {
            is BenchmarkRunState.Running -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoreCharcoalElevated)
                        .border(1.dp, CyanLuminous.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Running: ${state.currentCaseTitle}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanBright
                        )
                        Text(
                            text = "${state.currentCaseIndex} / ${state.totalCases}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateLight
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (state.totalCases > 0) state.currentCaseIndex.toFloat() / state.totalCases.toFloat() else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanLuminous,
                        trackColor = CoreCharcoalBorder
                    )

                    OutlinedButton(
                        onClick = onCancelBenchmark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_benchmark_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Cancel Benchmark", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            is BenchmarkRunState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            else -> {}
        }

        // 4. Action Buttons (Run / Reset)
        if (benchmarkState !is BenchmarkRunState.Running) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunBenchmark,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("run_benchmark_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanLuminous,
                        contentColor = CoreBlack
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeResult == null) "RUN BENCHMARK" else "RE-RUN BENCHMARK",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (activeResult != null) {
                    OutlinedButton(
                        onClick = onResetBenchmark,
                        modifier = Modifier.testTag("reset_benchmark_button"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoreCharcoalBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reset", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 5. Results Section (If available)
        if (activeResult != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BENCHMARK RESULTS & METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanLuminous,
                    letterSpacing = 1.sp
                )

                // Cold start card
                if (activeResult.coldStartMetrics != null) {
                    val cold = activeResult.coldStartMetrics
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoreCharcoalElevated)
                            .border(1.dp, CoreCharcoalBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Cold Start Timing",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateBright
                        )
                        BenchmarkInfoRow(label = "Model Initialization", value = "${cold.modelInitializationTimeMs} ms")
                        BenchmarkInfoRow(label = "First Inference Latency", value = "${cold.firstInferenceTimeMs} ms")
                        BenchmarkInfoRow(
                            label = "Total Cold Start",
                            value = "${cold.totalColdStartTimeMs} ms" + if (cold.wasAlreadyInitialized) " (already in RAM)" else ""
                        )
                    }
                }

                // Individual test cases
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (caseResult in activeResult.caseResults) {
                        BenchmarkCaseItem(caseResult = caseResult)
                    }
                }

                // Overall summary & diagnostic findings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoreCharcoalElevated)
                        .border(1.dp, CyanLuminous.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Engineering Diagnostic Assessment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanBright
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Avg Warm Latency", style = MaterialTheme.typography.labelSmall, color = SlateLight)
                            Text(
                                text = "${activeResult.averageWarmLatencyMs} ms",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SlateBright
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Throughput", style = MaterialTheme.typography.labelSmall, color = SlateLight)
                            Text(
                                text = if (activeResult.averageTokensPerSecond != null && activeResult.averageTokensPerSecond > 0.0) {
                                    "${String.format(Locale.US, "%.1f", activeResult.averageTokensPerSecond)} tok/s"
                                } else {
                                    "Token metrics unavailable"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SlateBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeResult.diagnosticAssessment,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateBright,
                        lineHeight = 18.sp
                    )
                }

                // Copy report button
                OutlinedButton(
                    onClick = {
                        val report = activeResult.toMarkdownReport()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("AI Benchmark Report", report)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Benchmark report copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_benchmark_report_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanLuminous.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "COPY BENCHMARK REPORT", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun BenchmarkCaseItem(caseResult: LocalAIBenchmarkCaseResult) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CoreCharcoalElevated)
            .border(
                1.dp,
                if (caseResult.isSuccess) CoreCharcoalBorder else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = caseResult.testCase.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateBright
                )
                Text(
                    text = caseResult.testCase.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanBright
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (caseResult.isSuccess) SuccessGreen.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = caseResult.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (caseResult.isSuccess) SuccessGreen else MaterialTheme.colorScheme.error
                )
            }
        }

        // Prompt
        Text(
            text = "Prompt: \"${caseResult.testCase.prompt}\"",
            style = MaterialTheme.typography.bodySmall,
            color = SlateLight
        )

        // Metrics row
        if (caseResult.metrics != null) {
            val m = caseResult.metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "TTFT: ${m.timeToFirstResponseMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateBright
                )
                Text(
                    text = "Total: ${m.totalInferenceTimeMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateBright
                )
                Text(
                    text = m.tokenMetricsSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (m.tokenMetricsAvailable) CyanBright else SlateMuted
                )
            }
        }

        // Output text
        if (caseResult.outputText.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoreBlack.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    text = if (expanded || caseResult.outputText.length <= 160) {
                        caseResult.outputText
                    } else {
                        caseResult.outputText.take(160) + "..."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = SlateBright
                )

                if (caseResult.outputText.length > 160) {
                    Text(
                        text = if (expanded) "Show less" else "Show full output (${caseResult.outputLength} chars)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanLuminous,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { expanded = !expanded }
                            .testTag("expand_output_${caseResult.testCase.id}")
                    )
                }
            }
        } else if (caseResult.errorMessage != null) {
            Text(
                text = "Error: ${caseResult.errorMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BenchmarkInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateLight
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = SlateBright
        )
    }
}
