package com.example.core.intent

import com.example.actions.ActionRequest

/**
 * Formal intent abstraction produced by language parsing.
 * Designed to be populated by local AI models in future phases.
 */
sealed interface SummerIntent {
    val rawQuery: String
    val confidence: Float

    data class Greeting(
        override val rawQuery: String,
        override val confidence: Float = 1.0f
    ) : SummerIntent

    data class IdentityQuestion(
        override val rawQuery: String,
        override val confidence: Float = 1.0f
    ) : SummerIntent

    data class CapabilityQuestion(
        override val rawQuery: String,
        override val confidence: Float = 1.0f
    ) : SummerIntent

    data class TimeQuery(
        override val rawQuery: String,
        override val confidence: Float = 1.0f
    ) : SummerIntent

    data class GeneralConversation(
        val message: String,
        override val rawQuery: String = message,
        override val confidence: Float = 0.8f
    ) : SummerIntent

    data class CapabilityAction(
        val actionRequest: ActionRequest,
        override val rawQuery: String,
        override val confidence: Float = 1.0f
    ) : SummerIntent

    data class Unknown(
        override val rawQuery: String,
        override val confidence: Float = 0.0f
    ) : SummerIntent
}
