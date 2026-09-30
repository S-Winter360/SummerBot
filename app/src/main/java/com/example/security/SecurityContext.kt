package com.example.security

data class SecurityContext(
    val caller: String,
    val isUserInitiated: Boolean = true,
    val sessionAuthorized: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
