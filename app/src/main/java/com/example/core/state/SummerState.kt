package com.example.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SummerState {
    val description: String
    val displayName: String
        get() = when (this) {
            is Idle -> "Idle"
            is Listening -> "Listening"
            is Thinking -> "Thinking"
            is Speaking -> "Speaking"
            is Executing -> "Executing"
            is Observing -> "Observing"
            is Learning -> "Learning"
            is Error -> "Error"
        }

    data object Idle : SummerState {
        override val description: String = "Observant, quiescent, and ready."
    }

    data class Listening(val inputPreview: String = "Awaiting speech or sensor input.") : SummerState {
        override val description: String = inputPreview
    }

    data class Thinking(val intent: String? = null) : SummerState {
        override val description: String = "Synthesizing intent and evaluating reasoning pipelines."
    }

    data class Speaking(val textPreview: String) : SummerState {
        override val description: String = "Rendering conversational response."
    }

    data class Executing(val actionName: String) : SummerState {
        override val description: String = "Authorizing and performing system action: $actionName."
    }

    data class Observing(val focus: String = "Ambient context") : SummerState {
        override val description: String = "Monitoring environmental and contextual inputs: $focus."
    }

    data class Learning(val topic: String) : SummerState {
        override val description: String = "Consolidating observation into local memory: $topic."
    }

    data class Error(val cause: String) : SummerState {
        override val description: String = "Safeguard fault intercepted: $cause."
    }
}

class SummerStateManager(initialState: SummerState = SummerState.Idle) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<SummerState> = _state.asStateFlow()

    fun transitionTo(newState: SummerState, cause: String = "") {
        _state.value = newState
    }
}
