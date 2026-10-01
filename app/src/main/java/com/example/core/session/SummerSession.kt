package com.example.core.session

import com.example.core.interaction.SummerInteraction

data class SummerSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startedTimestamp: Long = System.currentTimeMillis(),
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val interactions: List<SummerInteraction> = emptyList()
)
