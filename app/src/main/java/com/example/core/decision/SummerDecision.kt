package com.example.core.decision

import com.example.actions.ActionRequest
import com.example.core.response.MemoryOperation
import com.example.core.response.SummerResponse

data class SummerDecision(
    val response: SummerResponse,
    val actionRequests: List<ActionRequest> = emptyList(),
    val memoryOperations: List<MemoryOperation> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val rationale: String = "Default decision evaluation based on AI intent and active context."
)
