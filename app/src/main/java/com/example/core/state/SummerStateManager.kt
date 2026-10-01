package com.example.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StateTransitionRecord(
    val fromState: SummerState,
    val toState: SummerState,
    val cause: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class SummerStateManager(initialState: SummerState = SummerState.Idle) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<SummerState> = _state.asStateFlow()

    private val _transitionHistory = MutableStateFlow<List<StateTransitionRecord>>(emptyList())
    val transitionHistory: StateFlow<List<StateTransitionRecord>> = _transitionHistory.asStateFlow()

    fun transitionTo(newState: SummerState, cause: String = "") {
        val current = _state.value
        _state.value = newState
        val record = StateTransitionRecord(fromState = current, toState = newState, cause = cause)
        _transitionHistory.update { listOf(record) + it.take(99) }
    }

    fun resetToIdle() {
        transitionTo(SummerState.Idle, cause = "Reset to idle")
    }
}
