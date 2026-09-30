package com.example.core.context

import com.example.core.interaction.SummerInteraction
import com.example.core.state.SummerState
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.network.NetworkState

data class SummerContext(
    val sessionId: String,
    val recentInteractions: List<SummerInteraction> = emptyList(),
    val activeMemories: List<MemoryRecord> = emptyList(),
    val networkState: NetworkState = NetworkState.CONNECTED_WIFI,
    val currentState: SummerState = SummerState.Idle,
    val settings: SummerSettings = SummerSettings(),
    val timestamp: Long = System.currentTimeMillis()
)
