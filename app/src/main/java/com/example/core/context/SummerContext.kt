package com.example.core.context

import com.example.actions.ActionRequest
import com.example.actions.ActionResult
import com.example.core.interaction.SummerInteraction
import com.example.core.state.SummerState
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.network.NetworkState

/**
 * Immutable snapshot of contextual intelligence available to Summer
 * during interaction processing and decision making.
 */
data class SummerContext(
    val sessionId: String,
    val currentUser: String? = "Primary User",
    val recentConversation: List<SummerInteraction> = emptyList(),
    val relevantMemories: List<MemoryRecord> = emptyList(),
    val networkState: NetworkState = NetworkState.OFFLINE,
    val currentState: SummerState = SummerState.Idle,
    val settings: SummerSettings = SummerSettings(),
    val pendingActions: List<ActionRequest> = emptyList(),
    val recentActionResults: List<ActionResult> = emptyList(),
    val environmentalInfo: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
