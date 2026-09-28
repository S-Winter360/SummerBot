package com.example.core.state

/**
 * Represents the comprehensive state model for Summer.
 * Designed to support both current demonstration states and future
 * multimodal / autonomous processing states without architectural rewriting.
 */
sealed interface SummerState {
    val displayName: String
    val description: String

    /** Default standby state, awaiting interaction or wake word. */
    data object Idle : SummerState {
        override val displayName: String = "IDLE"
        override val description: String = "Standing by. Offline cognitive system ready."
    }

    /** Actively capturing user speech or audio input. */
    data class Listening(
        val inputSource: String = "Voice input"
    ) : SummerState {
        override val displayName: String = "LISTENING"
        override val description: String = "Receiving audio stream via $inputSource."
    }

    /** Processing intent, querying memory, or performing reasoning. */
    data class Thinking(
        val stage: String = "Analyzing contextual intent"
    ) : SummerState {
        override val displayName: String = "THINKING"
        override val description: String = stage
    }

    /** Synthesizing speech or presenting verbal/audio output. */
    data class Speaking(
        val utterance: String? = null
    ) : SummerState {
        override val displayName: String = "SPEAKING"
        override val description: String = utterance ?: "Responding to user."
    }

    /** Executing an authorized device action or planned sequence. */
    data class Executing(
        val actionName: String = "System Task"
    ) : SummerState {
        override val displayName: String = "EXECUTING"
        override val description: String = "Running authorized task: $actionName."
    }

    /** Actively observing visual or ambient contextual sensors. */
    data class Observing(
        val sensorType: String = "Ambient context"
    ) : SummerState {
        override val displayName: String = "OBSERVING"
        override val description: String = "Analyzing $sensorType."
    }

    /** Consolidating local episodic memory or updating knowledge weights. */
    data class Learning(
        val domain: String = "Personal preferences"
    ) : SummerState {
        override val displayName: String = "LEARNING"
        override val description: String = "Consolidating $domain into local memory."
    }

    /** State entered when a fault or unauthorized action occurs. */
    data class Error(
        val message: String
    ) : SummerState {
        override val displayName: String = "FAULT"
        override val description: String = message
    }
}
