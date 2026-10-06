package com.example.ui.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Presentation-level speaker representation in an in-session conversation.
 */
enum class ConversationSpeaker {
    USER,
    SUMMER
}

/**
 * Origin of the conversational turn.
 */
enum class ConversationSource {
    VOICE,
    TEXT,
    SYSTEM
}

/**
 * Presentation-level conversation message model for the in-session transcript.
 * Kept separate from permanent memory in MemoryRepository.
 */
data class ConversationMessage(
    val id: String = UUID.randomUUID().toString(),
    val speaker: ConversationSpeaker,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: ConversationSource = ConversationSource.VOICE
) {
    fun formatTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
