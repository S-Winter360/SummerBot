package com.example.actions

import com.example.security.Capability

data class ActionRequest(
    val capability: Capability,
    val actionName: String,
    val reasoning: String,
    val parameters: Map<String, String> = emptyMap(),
    val id: String = java.util.UUID.randomUUID().toString()
)
