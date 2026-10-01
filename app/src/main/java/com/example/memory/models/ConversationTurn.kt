package com.example.memory.models

enum class ConversationRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ConversationTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: ConversationRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
