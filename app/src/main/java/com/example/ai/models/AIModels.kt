package com.example.ai.models

import com.example.actions.ActionRequest
import com.example.core.intent.SummerIntent
import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryRecord

/**
 * Diagnostic and capability metadata for an AI inference engine.
 */
data class AIModelInfo(
    val name: String,
    val version: String,
    val isLocalOffline: Boolean = true,
    val description: String
)

/**
 * Contextual request sent to Summer's reasoning core.
 */
data class AIRequest(
    val query: String,
    val conversationId: String = "primary_session",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Categorization of recognized user intent (legacy / compatibility).
 */
sealed interface RecognizedIntent {
    data class SystemStatus(val query: String) : RecognizedIntent
    data class PersonalityQuery(val query: String) : RecognizedIntent
    data class CapabilityRequest(val actionRequest: ActionRequest) : RecognizedIntent
    data class GeneralConversation(val text: String) : RecognizedIntent
}

/**
 * Structured response produced by the AI Engine (legacy / compatibility).
 */
data class AIResponse(
    val text: String,
    val modelUsed: String,
    val recognizedIntent: RecognizedIntent,
    val requiredAction: ActionRequest? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Modern structured result produced by [com.example.ai.AIEngine] for [com.example.core.orchestrator.SummerOrchestrator].
 */
data class AIResult(
    val intent: SummerIntent,
    val response: SummerResponse,
    val actionRequests: List<ActionRequest> = emptyList(),
    val memorySuggestions: List<MemoryRecord> = emptyList(),
    val confidence: Float = 1.0f
)
