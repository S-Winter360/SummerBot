package com.example.core.context

import com.example.core.interaction.SummerInteraction
import com.example.core.state.SummerState
import com.example.memory.models.ConversationTurn
import com.example.memory.models.MemoryContext
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import com.example.network.NetworkState
import com.example.vision.VisionObservation

data class SummerContext(
    val sessionId: String,
    val recentInteractions: List<SummerInteraction> = emptyList(),
    val conversationTurns: List<ConversationTurn> = emptyList(),
    val activeMemories: List<MemoryRecord> = emptyList(),
    val memoryContext: MemoryContext? = null,
    val networkState: NetworkState = NetworkState.CONNECTED_WIFI,
    val currentState: SummerState = SummerState.Idle,
    val settings: SummerSettings = SummerSettings(),
    val currentVisionObservation: VisionObservation? = null,
    val timestamp: Long = System.currentTimeMillis()
)
