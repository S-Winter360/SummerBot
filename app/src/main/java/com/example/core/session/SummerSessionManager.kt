package com.example.core.session

import com.example.core.interaction.SummerInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SummerSessionManager {
    private val _currentSession = MutableStateFlow(SummerSession())
    val currentSession: StateFlow<SummerSession> = _currentSession.asStateFlow()

    private val sessionHistory = mutableListOf<SummerSession>()

    fun getActiveSession(): SummerSession = _currentSession.value

    fun recordInteraction(interaction: SummerInteraction) {
        _currentSession.update { current ->
            current.copy(
                lastActiveTimestamp = System.currentTimeMillis(),
                interactions = current.interactions + interaction
            )
        }
    }

    fun startNewSession(): SummerSession {
        val old = _currentSession.value
        if (old.interactions.isNotEmpty()) {
            sessionHistory.add(old)
        }
        val fresh = SummerSession()
        _currentSession.value = fresh
        return fresh
    }

    fun getSessionHistory(): List<SummerSession> = sessionHistory.toList()
}
