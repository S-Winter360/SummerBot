package com.example.security

sealed interface AuthorizationResult {
    val isAllowed: Boolean

    data class Granted(
        val reason: String = "Policy verified and permitted"
    ) : AuthorizationResult {
        override val isAllowed: Boolean = true
    }

    data class Denied(
        val reason: String
    ) : AuthorizationResult {
        override val isAllowed: Boolean = false
    }

    data class RequiresUserConsent(
        val prompt: String,
        val rationale: String
    ) : AuthorizationResult {
        override val isAllowed: Boolean = false
    }
}
