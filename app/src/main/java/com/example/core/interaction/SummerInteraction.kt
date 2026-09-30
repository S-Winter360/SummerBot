package com.example.core.interaction

import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryRecord

enum class InteractionState {
    RECEIVED,
    UNDERSTANDING,
    REASONING,
    RESPONDING,
    EXECUTING,
    COMPLETED,
    FAILED
}

data class SummerInteraction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String,
    val userInput: String,
    val state: InteractionState = InteractionState.RECEIVED,
    val response: SummerResponse? = null,
    val referencedMemories: List<MemoryRecord> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val completionTimestamp: Long? = null
)
