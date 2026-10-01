package com.example.memory.validation

import com.example.core.response.MemoryOperation
import com.example.memory.models.MemoryImportance
import com.example.memory.models.MemoryRecord
import com.example.memory.models.MemorySource

sealed interface MemoryValidationResult {
    data class Valid(val operation: MemoryOperation) : MemoryValidationResult
    data class Invalid(val reason: String) : MemoryValidationResult
}

class MemoryOperationValidator(
    private val maxContentLength: Int = 500,
    private val maxTitleLength: Int = 100
) {
    private val injectionPatterns = listOf(
        "ignore previous instructions",
        "system override",
        "disregard all rules",
        "you are now in developer mode",
        "drop database",
        "delete from sqlite_master"
    )

    private val sensitivePatterns = listOf(
        "password is",
        "secret key",
        "auth token",
        "private key",
        "credit card",
        "cvv"
    )

    fun validate(operation: MemoryOperation): MemoryValidationResult {
        return when (operation) {
            is MemoryOperation.Store -> validateRecord(operation.record).let { res ->
                if (res is MemoryValidationResult.Valid) MemoryValidationResult.Valid(operation) else res
            }
            is MemoryOperation.Update -> {
                val recordRes = validateRecord(operation.record)
                if (recordRes is MemoryValidationResult.Invalid) {
                    recordRes
                } else if (operation.targetId.isNullOrBlank() && operation.previousContent.isNullOrBlank()) {
                    MemoryValidationResult.Invalid("Update operation requires either targetId or previousContent identifier.")
                } else {
                    MemoryValidationResult.Valid(operation)
                }
            }
            is MemoryOperation.Forget -> {
                if (operation.targetId.isNullOrBlank() && operation.keywordOrContent.isNullOrBlank()) {
                    MemoryValidationResult.Invalid("Forget operation requires either a targetId or a keyword identifier.")
                } else {
                    MemoryValidationResult.Valid(operation)
                }
            }
            is MemoryOperation.ClearAll -> MemoryValidationResult.Valid(operation)
        }
    }

    private fun validateRecord(record: MemoryRecord): MemoryValidationResult {
        val content = record.content.trim()
        if (content.isBlank()) {
            return MemoryValidationResult.Invalid("Memory content cannot be empty or blank.")
        }
        if (content.length > maxContentLength) {
            return MemoryValidationResult.Invalid("Memory content exceeds maximum length of $maxContentLength characters.")
        }
        if (record.title.length > maxTitleLength) {
            return MemoryValidationResult.Invalid("Memory title exceeds maximum length of $maxTitleLength characters.")
        }

        val lowerContent = content.lowercase()

        // Check for prompt injection attempts
        for (pattern in injectionPatterns) {
            if (lowerContent.contains(pattern)) {
                return MemoryValidationResult.Invalid("Memory content contains forbidden system-override instruction.")
            }
        }

        // Check for raw sensitive secrets
        for (pattern in sensitivePatterns) {
            if (lowerContent.contains(pattern)) {
                return MemoryValidationResult.Invalid("Memory content contains unencrypted credential pattern.")
            }
        }

        val clampedConfidence = record.confidence.coerceIn(0.0f, 1.0f)
        val sanitizedRecord = record.copy(
            content = content,
            title = record.title.trim(),
            confidence = clampedConfidence
        )

        return MemoryValidationResult.Valid(MemoryOperation.Store(sanitizedRecord))
    }
}
