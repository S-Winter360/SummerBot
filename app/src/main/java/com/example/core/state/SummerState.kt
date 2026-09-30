package com.example.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SummerState {
    val description: String

    data object Idle : SummerState {
        override val description: String = "Observant, quiescent, and ready."
    }

    data object Listening : SummerState {
        override val description: String = "Awaiting speech or sensor input."
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

    data class Learning(val topic: String) : SummerState {
        override val description: String = "Consolidating observation into local memory."
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
