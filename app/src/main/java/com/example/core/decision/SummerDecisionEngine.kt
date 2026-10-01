package com.example.core.decision

import com.example.ai.models.AIResult
import com.example.core.context.SummerContext
import com.example.core.response.MemoryOperation

interface SummerDecisionEngine {
    fun decide(aiResult: AIResult, context: SummerContext): SummerDecision
}

class DefaultSummerDecisionEngine : SummerDecisionEngine {
    override fun decide(aiResult: AIResult, context: SummerContext): SummerDecision {
        val memoryOps = aiResult.memorySuggestions.map { MemoryOperation.Store(it) }

        val requiresConfirmation = aiResult.actionRequests.any {
            it.capability.requiresUserConfirmation
        }

        return SummerDecision(
            response = aiResult.response,
            actionRequests = aiResult.actionRequests,
            memoryOperations = memoryOps,
            requiresConfirmation = requiresConfirmation,
            rationale = "Generated from intent ${aiResult.intent::class.java.simpleName} with confidence ${aiResult.confidence}"
        )
    }
}
