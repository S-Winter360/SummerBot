package com.example.ai

import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent

interface AIEngine {
    val modelInfo: AIModelInfo
    val isReady: Boolean

    suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult
    suspend fun classifyIntent(input: String): SummerIntent
    suspend fun processQuery(request: AIRequest): AIResponse
    suspend fun evaluateIntent(input: String): RecognizedIntent
}
