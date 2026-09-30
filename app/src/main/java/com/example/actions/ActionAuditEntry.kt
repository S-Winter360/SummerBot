package com.example.actions

import com.example.security.Capability

data class ActionAuditEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionName: String,
    val capability: Capability,
    val authorized: Boolean,
    val caller: String,
    val outcomeSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)
