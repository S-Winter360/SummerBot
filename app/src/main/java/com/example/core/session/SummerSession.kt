package com.example.core.session

import com.example.core.interaction.SummerInteraction
import java.util.UUID

/**
 * Encapsulates an active conversational session with Summer.
 */
data class SummerSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val lastActivityTime: Long = System.currentTimeMillis(),
    val interactions: List<SummerInteraction> = emptyList(),
    val isActive: Boolean = true
) {
    val interactionCount: Int
        get() = interactions.size

    val latestInteraction: SummerInteraction?
        get() = interactions.lastOrNull()
}
