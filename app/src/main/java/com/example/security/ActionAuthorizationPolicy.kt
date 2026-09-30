package com.example.security

import com.example.actions.ActionRequest

interface ActionAuthorizationPolicy {
    fun isAuthorized(request: ActionRequest, context: SecurityContext): Boolean
    fun requiresExplicitUserConfirmation(request: ActionRequest): Boolean
}

class DefaultActionAuthorizationPolicy : ActionAuthorizationPolicy {
    override fun isAuthorized(request: ActionRequest, context: SecurityContext): Boolean {
        if (!context.sessionAuthorized) return false
        return when (request.capability) {
            Capability.INTERNET, Capability.MICROPHONE, Capability.STORAGE, Capability.SPEECH_SYNTHESIS -> true
            Capability.CAMERA, Capability.SYSTEM_SETTINGS -> context.isUserInitiated
        }
    }

    override fun requiresExplicitUserConfirmation(request: ActionRequest): Boolean {
        return request.capability.requiresUserConfirmation
    }
}
