package com.example.core.interaction

import com.example.actions.ActionRequest
import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryRecord
import java.util.UUID

/**
 * Immutable representation of a distinct conversational or operational interaction.
 */
data class SummerInteraction(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val inputSource: String = "text",
    val userInput: String,
    val processingState: InteractionState = InteractionState.RECEIVED,
    val response: SummerResponse? = null,
    val referencedMemories: List<MemoryRecord> = emptyList(),
    val actionRequests: List<ActionRequest> = emptyList(),
    val errorMessage: String? = null,
    val completedTimestamp: Long? = null
) {
    val isCompleted: Boolean
        get() = processingState == InteractionState.COMPLETED

    val isFailed: Boolean
        get() = processingState == InteractionState.FAILED
}
