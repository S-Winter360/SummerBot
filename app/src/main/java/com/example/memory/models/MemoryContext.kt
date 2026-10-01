package com.example.memory.models

data class MemoryContext(
    val relevantMemories: List<MemoryRecord> = emptyList(),
    val recentConversation: List<ConversationTurn> = emptyList(),
    val activeSessionId: String,
    val retrievalMetadata: Map<String, String> = emptyMap()
)
