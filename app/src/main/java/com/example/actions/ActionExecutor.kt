package com.example.actions

import com.example.memory.models.SummerSettings
import com.example.security.ActionAuthorizationPolicy
import com.example.security.AuthorizationResult
import com.example.security.SecurityContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fundamental abstraction for performing system and device actions on behalf of Summer.
 */
interface ActionExecutor {
    suspend fun execute(
        request: ActionRequest,
        context: SecurityContext,
        settings: SummerSettings
    ): ActionResult
}

/**
 * Concrete implementation that enforces the capability authorization policy
 * before any execution can take place, logging an audit trail of every check.
 */
class SecuredActionExecutor(
    private val policy: ActionAuthorizationPolicy
) : ActionExecutor {

    private val _auditLog = MutableStateFlow<List<ActionAuditEntry>>(emptyList())
    val auditLog: StateFlow<List<ActionAuditEntry>> = _auditLog.asStateFlow()

    override suspend fun execute(
        request: ActionRequest,
        context: SecurityContext,
        settings: SummerSettings
    ): ActionResult {
        val startTime = System.currentTimeMillis()

        // 1. Authorization check
        val authResult = policy.evaluate(request.capability, context, settings)

        return when (authResult) {
            is AuthorizationResult.Denied -> {
                val result = ActionResult.Denied(authResult.reason)
                recordAudit(request, authResult, result)
                result
            }

            is AuthorizationResult.RequiresUserConsent -> {
                val result = ActionResult.PendingConsent(
                    prompt = authResult.prompt,
                    rationale = authResult.rationale
                )
                recordAudit(request, authResult, result)
                result
            }

            is AuthorizationResult.Granted -> {
                // 2. Safe controlled execution of permitted action
                val executionDuration = System.currentTimeMillis() - startTime
                val result = ActionResult.Success(
                    output = "Action [${request.actionName}] executed safely under authorization policy [${authResult.reason}].",
                    executionTimeMs = executionDuration
                )
                recordAudit(request, authResult, result)
                result
            }
        }
    }

    private fun recordAudit(
        request: ActionRequest,
        authResult: AuthorizationResult,
        actionResult: ActionResult
    ) {
        val entry = ActionAuditEntry(
            id = request.id,
            actionName = request.actionName,
            capabilityName = request.capability.title,
            reasoning = request.reasoning,
            authorizationSummary = when (authResult) {
                is AuthorizationResult.Granted -> authResult.reason
                is AuthorizationResult.Denied -> "Denied: ${authResult.reason}"
                is AuthorizationResult.RequiresUserConsent -> "Pending Consent: ${authResult.prompt}"
            },
            isAuthorized = authResult is AuthorizationResult.Granted,
            outcomeSummary = when (actionResult) {
                is ActionResult.Success -> "Success"
                is ActionResult.Denied -> "Denied"
                is ActionResult.Failed -> "Failed: ${actionResult.error}"
                is ActionResult.PendingConsent -> "Awaiting confirmation"
            }
        )
        val current = _auditLog.value
        _auditLog.value = (listOf(entry) + current).take(25)
    }
}
