package com.example.actions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result returned from the ActionExecutor pipeline.
 */
sealed interface ActionResult {
    val isSuccess: Boolean

    data class Success(
        val output: String,
        val executionTimeMs: Long = 0L
    ) : ActionResult {
        override val isSuccess: Boolean = true
    }

    data class Denied(
        val reason: String
    ) : ActionResult {
        override val isSuccess: Boolean = false
    }

    data class Failed(
        val error: String
    ) : ActionResult {
        override val isSuccess: Boolean = false
    }

    data class PendingConsent(
        val prompt: String,
        val rationale: String
    ) : ActionResult {
        override val isSuccess: Boolean = false
    }
}

/**
 * Audit entry verifying the complete security pipeline trace:
 * AI decision -> action request -> authorization check -> action executor -> result.
 */
data class ActionAuditEntry(
    val id: String,
    val actionName: String,
    val capabilityName: String,
    val reasoning: String,
    val authorizationSummary: String,
    val isAuthorized: Boolean,
    val outcomeSummary: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
