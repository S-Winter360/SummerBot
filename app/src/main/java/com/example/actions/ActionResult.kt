package com.example.actions

sealed interface ActionResult {
    val isSuccess: Boolean
    val message: String

    data class Success(override val message: String, val payload: Any? = null) : ActionResult {
        override val isSuccess: Boolean = true
    }

    data class Denied(override val message: String, val reason: String) : ActionResult {
        override val isSuccess: Boolean = false
    }

    data class Failure(override val message: String, val error: Throwable? = null) : ActionResult {
        override val isSuccess: Boolean = false
    }
}
