package com.example.memory.models

/**
 * Architectural categories of local memory that Summer is designed to retain.
 */
enum class MemoryCategory(val displayName: String) {
    USER_PREFERENCE("User Preference"),
    ASSISTANT_PERSONALITY("Assistant Personality"),
    CONVERSATION_MEMORY("Conversation Memory"),
    LEARNED_FACT("Learned Fact"),
    PERSON_PROFILE("Person Profile"),
    EPISODIC_EVENT("Episodic Event")
}

/**
 * Fundamental data unit for Summer's future local episodic and factual recall.
 */
data class MemoryRecord(
    val id: Long = 0L,
    val category: MemoryCategory,
    val title: String,
    val content: String,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)
