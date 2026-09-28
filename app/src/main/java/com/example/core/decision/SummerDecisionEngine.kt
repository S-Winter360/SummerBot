package com.example.core.decision

import com.example.ai.models.AIResult
import com.example.core.context.SummerContext
import com.example.core.response.MemoryOperation
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.security.Capability

/**
 * Fundamental abstraction responsible for deciding what Summer should do with an interpreted interaction.
 * Evaluates AI results in conjunction with context snapshots, capability limits, and memory policies.
 */
interface SummerDecisionEngine {
    suspend fun decide(
        aiResult: AIResult,
        context: SummerContext
    ): SummerDecision
}

/**
 * Default implementation of [SummerDecisionEngine].
 * Enforces architectural boundaries:
 * 1. Checks master enablement.
 * 2. Prepares action requests for authorization without executing them directly.
 * 3. Schedules memory persistence only when allowed by settings.
 * 4. Yields structured decision metadata without exposing private chain-of-thought.
 */
class DefaultSummerDecisionEngine : SummerDecisionEngine {

    override suspend fun decide(
        aiResult: AIResult,
        context: SummerContext
    ): SummerDecision {
        // Master switch check
        if (!context.settings.summerEnabled) {
            return SummerDecision(
                response = SummerResponse(
                    text = "Summer is currently disabled in system settings. Enable it to resume interactions.",
                    type = ResponseType.ERROR,
                    confidence = 1.0f,
                    source = "Summer Decision Engine"
                ),
                confidence = 1.0f,
                metadata = mapOf("status" to "disabled_by_user")
            )
        }

        val actionRequests = aiResult.actionRequests
        val requiresConfirmation = actionRequests.any {
            it.capability == Capability.CAMERA || it.capability == Capability.DEVICE_SETTINGS
        }

        val memoryOperations = mutableListOf<MemoryOperation>()
        if (context.settings.personalMemoryEnabled) {
            aiResult.memorySuggestions.forEach { record ->
                memoryOperations.add(MemoryOperation.Store(record))
            }
        }

        val decisionMetadata = mapOf(
            "intent" to aiResult.intent::class.java.simpleName,
            "confidence" to String.format(java.util.Locale.US, "%.2f", aiResult.confidence),
            "requiresConfirmation" to requiresConfirmation.toString(),
            "actionCount" to actionRequests.size.toString(),
            "memoryOpCount" to memoryOperations.size.toString()
        )

        return SummerDecision(
            response = aiResult.response,
            actionRequests = actionRequests,
            memoryOperations = memoryOperations,
            requiresConfirmation = requiresConfirmation,
            confidence = aiResult.confidence,
            metadata = decisionMetadata
        )
    }
}
