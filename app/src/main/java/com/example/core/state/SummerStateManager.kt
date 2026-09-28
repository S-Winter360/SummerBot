package com.example.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Audit record representing a state change event in Summer's lifecycle.
 */
data class StateTransitionRecord(
    val previousState: SummerState,
    val newState: SummerState,
    val timestamp: Long = System.currentTimeMillis(),
    val cause: String = "Internal transition"
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Central state coordinator for Summer.
 * Exposes observable StateFlow and enforces structured state transitions.
 */
class SummerStateManager {

    private val _state = MutableStateFlow<SummerState>(SummerState.Idle)
    val state: StateFlow<SummerState> = _state.asStateFlow()

    private val _transitionHistory = MutableStateFlow<List<StateTransitionRecord>>(emptyList())
    val transitionHistory: StateFlow<List<StateTransitionRecord>> = _transitionHistory.asStateFlow()

    /**
     * Transition Summer to a new operational state.
     */
    fun transitionTo(newState: SummerState, cause: String = "User interaction") {
        val current = _state.value
        if (current == newState) return

        val record = StateTransitionRecord(
            previousState = current,
            newState = newState,
            cause = cause
        )

        _state.value = newState
        val updatedHistory = (_transitionHistory.value + record).takeLast(20)
        _transitionHistory.value = updatedHistory
    }

    /**
     * Reset to default Idle state.
     */
    fun resetToIdle(cause: String = "Routine idle reset") {
        transitionTo(SummerState.Idle, cause)
    }
}
