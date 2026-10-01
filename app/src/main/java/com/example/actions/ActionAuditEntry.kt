package com.example.actions

import com.example.security.Capability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActionAuditEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionName: String,
    val capability: Capability,
    val authorized: Boolean,
    val caller: String,
    val outcomeSummary: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isAuthorized: Boolean get() = authorized
    val capabilityName: String get() = capability.title
    val authorizationSummary: String get() = if (authorized) "Authorized for $caller" else "Denied: policy constraint"
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
