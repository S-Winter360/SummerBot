package com.example.core.response

import com.example.actions.ActionRequest

enum class ResponseType {
    TEXT,
    QUESTION,
    CONFIRMATION,
    INFORMATION,
    ACTION_PROPOSAL,
    ERROR
}

data class SummerResponse(
    val text: String,
    val type: ResponseType = ResponseType.TEXT,
    val suggestedActions: List<ActionRequest> = emptyList(),
    val memoryOperations: List<MemoryOperation> = emptyList(),
    val confidence: Float = 1.0f,
    val source: String = "Summer Decision Engine",
    val timestamp: Long = System.currentTimeMillis()
)
