package com.example.ai

import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.RecognizedIntent

/**
 * Fundamental abstraction for AI inference in Summer.
 * Decouples the reasoning backend (local on-device quantized model, future executor)
 * from the app UI and state managers.
 */
interface AIEngine {
    val modelInfo: AIModelInfo
    val isReady: Boolean

    suspend fun processQuery(request: AIRequest): AIResponse

    suspend fun evaluateIntent(input: String): RecognizedIntent
}
