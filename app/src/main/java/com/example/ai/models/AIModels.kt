package com.example.ai.models

import com.example.actions.ActionRequest
import com.example.core.intent.SummerIntent
import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryRecord

data class AIModelInfo(
    val name: String,
    val version: String,
    val isLocalOffline: Boolean = true,
    val description: String
)

data class AIRequest(
    val query: String,
    val conversationId: String = "primary_session",
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface RecognizedIntent {
    data class SystemStatus(val query: String) : RecognizedIntent
    data class PersonalityQuery(val query: String) : RecognizedIntent
    data class CapabilityRequest(val actionRequest: ActionRequest) : RecognizedIntent
    data class GeneralConversation(val text: String) : RecognizedIntent
}

data class AIResponse(
    val text: String,
    val modelUsed: String,
    val recognizedIntent: RecognizedIntent,
    val requiredAction: ActionRequest? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AIResult(
    val intent: SummerIntent,
    val response: SummerResponse,
    val actionRequests: List<ActionRequest> = emptyList(),
    val memorySuggestions: List<MemoryRecord> = emptyList(),
    val confidence: Float = 1.0f
)
