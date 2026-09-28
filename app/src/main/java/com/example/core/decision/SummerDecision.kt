package com.example.core.decision

import com.example.actions.ActionRequest
import com.example.core.response.MemoryOperation
import com.example.core.response.SummerResponse

/**
 * Output of the decision phase detailing next actions, memory mutations,
 * and conversational response before execution and security checks.
 */
data class SummerDecision(
    val response: SummerResponse,
    val actionRequests: List<ActionRequest> = emptyList(),
    val memoryOperations: List<MemoryOperation> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val confidence: Float = 1.0f,
    val metadata: Map<String, String> = emptyMap()
)
