package com.example.core.response

import com.example.actions.ActionRequest
import java.util.UUID

/**
 * Functional classification of an assistant response.
 */
enum class ResponseType {
    TEXT,
    QUESTION,
    CONFIRMATION,
    INFORMATION,
    ACTION_PROPOSAL,
    ERROR
}

/**
 * Structured response produced by Summer's cognitive engine.
 * Extensible to encapsulate speech synthesis guidance, UI hints, and action requests.
 */
data class SummerResponse(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: ResponseType = ResponseType.TEXT,
    val confidence: Float = 1.0f,
    val isSpeechAppropriate: Boolean = false,
    val isUiDisplayAppropriate: Boolean = true,
    val actionRequests: List<ActionRequest> = emptyList(),
    val memoryOperations: List<MemoryOperation> = emptyList(),
    val source: String = "Summer Cognitive Core",
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
