package com.example.core.session

import com.example.core.interaction.SummerInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages conversational sessions and interaction history across Summer's runtime.
 */
class SummerSessionManager {

    private val _currentSession = MutableStateFlow(SummerSession())
    val currentSession: StateFlow<SummerSession> = _currentSession.asStateFlow()

    private val _pastSessions = MutableStateFlow<List<SummerSession>>(emptyList())
    val pastSessions: StateFlow<List<SummerSession>> = _pastSessions.asStateFlow()

    /**
     * Initializes a fresh session and stores the current one into history if not empty.
     */
    fun startNewSession(): SummerSession {
        val old = _currentSession.value
        if (old.interactions.isNotEmpty()) {
            _pastSessions.update { (listOf(old.copy(isActive = false)) + it).take(20) }
        }
        val fresh = SummerSession()
        _currentSession.value = fresh
        return fresh
    }

    /**
     * Records or updates an interaction within the active session.
     */
    fun recordInteraction(interaction: SummerInteraction) {
        _currentSession.update { session ->
            val existingIndex = session.interactions.indexOfFirst { it.id == interaction.id }
            val updatedList = if (existingIndex >= 0) {
                session.interactions.toMutableList().apply { set(existingIndex, interaction) }
            } else {
                session.interactions + interaction
            }
            session.copy(
                interactions = updatedList,
                lastActivityTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * Updates the last activity timestamp of the active session.
     */
    fun touchSession() {
        _currentSession.update { it.copy(lastActivityTime = System.currentTimeMillis()) }
    }

    /**
     * Closes the active session.
     */
    fun closeSession() {
        _currentSession.update { it.copy(isActive = false) }
    }
}
