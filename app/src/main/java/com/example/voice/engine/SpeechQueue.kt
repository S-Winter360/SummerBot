package com.example.voice.engine

import com.example.voice.models.VoiceProfile
import java.util.UUID

data class SpeechItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val profile: VoiceProfile,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Bounded, interruptible speech queue preventing overlapping speech and unbounded growth.
 * Implements conversational interruptibility: when a new user interaction arrives,
 * obsolete queued items are instantly discarded.
 */
class SpeechQueue(
    private val maxQueueSize: Int = 10
) {
    private val queue = ArrayDeque<SpeechItem>()
    private val lock = Any()

    val size: Int
        get() = synchronized(lock) { queue.size }

    val isEmpty: Boolean
        get() = synchronized(lock) { queue.isEmpty() }

    fun enqueue(item: SpeechItem): Boolean = synchronized(lock) {
        if (queue.size >= maxQueueSize) {
            // Evict oldest item to prevent unbounded memory growth
            queue.removeFirstOrNull()
        }
        queue.addLast(item)
        true
    }

    fun poll(): SpeechItem? = synchronized(lock) {
        queue.removeFirstOrNull()
    }

    fun peek(): SpeechItem? = synchronized(lock) {
        queue.firstOrNull()
    }

    fun clear(): Int = synchronized(lock) {
        val clearedCount = queue.size
        queue.clear()
        clearedCount
    }

    fun interruptAndEnqueue(newItem: SpeechItem): Boolean = synchronized(lock) {
        queue.clear()
        queue.addLast(newItem)
        true
    }

    fun toList(): List<SpeechItem> = synchronized(lock) {
        queue.toList()
    }
}
