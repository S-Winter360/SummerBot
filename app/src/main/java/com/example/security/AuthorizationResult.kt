package com.example.security

/**
 * Contextual metadata accompanying every capability authorization evaluation.
 */
data class SecurityContext(
    val caller: String = "AI_REASONING_CORE",
    val isUserInitiated: Boolean = true,
    val isAppInForeground: Boolean = true,
    val requestedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Result of evaluating a capability request against system policies and user permissions.
 */
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
