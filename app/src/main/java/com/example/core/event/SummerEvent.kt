package com.example.core.event

import com.example.actions.ActionResult
import com.example.network.NetworkState

enum class EventPriority {
    LOW, NORMAL, HIGH, CRITICAL
}

sealed interface SummerEvent {
    val id: String
    val timestamp: Long
    val priority: EventPriority
    val source: String
    val sessionId: String?

    data class UserTextInput(
        val text: String,
        override val source: String = "ui.text_input",
        override val sessionId: String? = null,
        override val priority: EventPriority = EventPriority.NORMAL,
        override val timestamp: Long = System.currentTimeMillis(),
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : SummerEvent

    data class SystemEvent(
        val eventName: String,
        val details: String,
        override val source: String = "system.core",
        override val sessionId: String? = null,
        override val priority: EventPriority = EventPriority.NORMAL,
        override val timestamp: Long = System.currentTimeMillis(),
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : SummerEvent

    data class NetworkStateChanged(
        val newState: NetworkState,
        override val source: String = "system.network",
        override val sessionId: String? = null,
        override val priority: EventPriority = EventPriority.LOW,
        override val timestamp: Long = System.currentTimeMillis(),
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : SummerEvent

    data class VoiceInput(
        val audioDataPreview: String,
        override val source: String = "voice.engine",
        override val sessionId: String? = null,
        override val priority: EventPriority = EventPriority.HIGH,
        override val timestamp: Long = System.currentTimeMillis(),
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : SummerEvent

    data class VisionInput(
        val frameSummary: String,
        override val source: String = "vision.engine",
        override val sessionId: String? = null,
        override val priority: EventPriority = EventPriority.LOW,
        override val timestamp: Long = System.currentTimeMillis(),
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : SummerEvent

    data class ActionResultEvent(
        val actionName: String,
        val result: ActionResult,
        override val source: String = "action.executor",
        override val sessionId: String? = null,
        override val priority: EventPriority = EventPriority.NORMAL,
        override val timestamp: Long = System.currentTimeMillis(),
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : SummerEvent
}
