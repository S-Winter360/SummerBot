package com.example.core.event

import com.example.actions.ActionResult
import com.example.network.NetworkState
import java.util.UUID

/**
 * Priority levels for events entering the Summer cognitive pipeline.
 */
enum class EventPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Typed internal event representation for Summer's cognitive architecture.
 * Designed to capture events from diverse perception and system channels.
 */
sealed interface SummerEvent {
    val id: String
    val timestamp: Long
    val source: String
    val sessionId: String
    val priority: EventPriority
    val originatingUser: String?

    /**
     * User submitted text input via the conversation interface.
     */
    data class UserTextInput(
        val text: String,
        override val sessionId: String,
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "ui.text_input",
        override val priority: EventPriority = EventPriority.NORMAL,
        override val originatingUser: String? = "Primary User"
    ) : SummerEvent

    /**
     * Internal lifecycle or system telemetry event.
     */
    data class SystemEvent(
        val eventName: String,
        val details: String = "",
        override val sessionId: String,
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "system.core",
        override val priority: EventPriority = EventPriority.LOW,
        override val originatingUser: String? = null
    ) : SummerEvent

    /**
     * Device connectivity state transition event.
     */
    data class NetworkStateChanged(
        val newState: NetworkState,
        override val sessionId: String,
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "system.network",
        override val priority: EventPriority = EventPriority.NORMAL,
        override val originatingUser: String? = null
    ) : SummerEvent

    /**
     * Speech recognition input (for future voice pipeline).
     */
    data class VoiceInput(
        val transcript: String,
        val confidence: Float = 1.0f,
        override val sessionId: String,
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "sensor.voice",
        override val priority: EventPriority = EventPriority.HIGH,
        override val originatingUser: String? = null
    ) : SummerEvent

    /**
     * Vision / camera frame detection input (for future vision pipeline).
     */
    data class VisionInput(
        val description: String,
        override val sessionId: String,
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "sensor.vision",
        override val priority: EventPriority = EventPriority.NORMAL,
        override val originatingUser: String? = null
    ) : SummerEvent

    /**
     * Scheduled reminder or timer elapsed event.
     */
    data class TimerEvent(
        val timerId: String,
        val description: String,
        override val sessionId: String,
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "system.timer",
        override val priority: EventPriority = EventPriority.HIGH,
        override val originatingUser: String? = null
    ) : SummerEvent

    /**
     * Outcome notification for an executed device action.
     */
    data class ActionResultEvent(
        val actionId: String,
        val result: ActionResult,
        override val sessionId: String,
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "security.action_executor",
        override val priority: EventPriority = EventPriority.NORMAL,
        override val originatingUser: String? = null
    ) : SummerEvent
}
