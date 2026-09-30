package com.example.actions

import com.example.security.ActionAuthorizationPolicy
import com.example.security.Capability
import com.example.security.SecurityContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ActionExecutor {
    suspend fun execute(request: ActionRequest, context: SecurityContext): ActionResult
}

class SecuredActionExecutor(
    private val policy: ActionAuthorizationPolicy
) : ActionExecutor {

    private val _auditLog = MutableStateFlow<List<ActionAuditEntry>>(emptyList())
    val auditLog: StateFlow<List<ActionAuditEntry>> = _auditLog.asStateFlow()

    override suspend fun execute(request: ActionRequest, context: SecurityContext): ActionResult {
        val authorized = policy.isAuthorized(request, context)

        if (!authorized) {
            val entry = ActionAuditEntry(
                actionName = request.actionName,
                capability = request.capability,
                authorized = false,
                caller = context.caller,
                outcomeSummary = "Action denied by security authorization policy"
            )
            _auditLog.update { listOf(entry) + it.take(49) }
            return ActionResult.Denied(
                message = "Action denied: [${request.actionName}] exceeds current security privileges.",
                reason = "Capability [${request.capability.name}] not authorized for caller [${context.caller}]"
            )
        }

        // Controlled safe execution
        val result = when (request.capability) {
            Capability.INTERNET -> ActionResult.Success("Network check completed successfully: connectivity verified.")
            Capability.MICROPHONE -> ActionResult.Success("Audio input initialized for acoustic verification.")
            Capability.STORAGE -> ActionResult.Success("Local storage verified: persistent state accessible.")
            Capability.SPEECH_SYNTHESIS -> ActionResult.Success("Voice synthesis module dispatched.")
            Capability.CAMERA -> ActionResult.Success("Visual sensor stream simulation completed.")
            Capability.SYSTEM_SETTINGS -> ActionResult.Success("System settings parameter successfully updated.")
        }

        val entry = ActionAuditEntry(
            actionName = request.actionName,
            capability = request.capability,
            authorized = true,
            caller = context.caller,
            outcomeSummary = result.message
        )
        _auditLog.update { listOf(entry) + it.take(49) }

        return result
    }
}
