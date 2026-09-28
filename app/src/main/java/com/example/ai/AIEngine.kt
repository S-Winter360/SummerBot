package com.example.ai

import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent

/**
 * Fundamental abstraction for AI inference in Summer.
 * Decouples the reasoning backend (local on-device quantized model, future executor)
 * from the app UI and state managers.
 */
interface AIEngine {
    val modelInfo: AIModelInfo
    val isReady: Boolean

    /**
     * Modern cognitive processing: evaluates interaction within a rich context snapshot
     * and yields a structured AIResult.
     */
    suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult

    /**
     * Classifies natural language input into a structured [SummerIntent].
     */
    suspend fun classifyIntent(input: String): SummerIntent

    /**
     * Legacy / simple query processing for backward compatibility.
     */
    suspend fun processQuery(request: AIRequest): AIResponse

    suspend fun evaluateIntent(input: String): RecognizedIntent
}
