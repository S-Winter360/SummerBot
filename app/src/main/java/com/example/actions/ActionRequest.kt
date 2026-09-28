package com.example.actions

import com.example.security.Capability
import java.util.UUID

/**
 * Structured request generated when an AI decision requires interaction with device resources.
 */
data class ActionRequest(
    val id: String = UUID.randomUUID().toString(),
    val capability: Capability,
    val actionName: String,
    val parameters: Map<String, String> = emptyMap(),
    val reasoning: String,
    val timestamp: Long = System.currentTimeMillis()
)
