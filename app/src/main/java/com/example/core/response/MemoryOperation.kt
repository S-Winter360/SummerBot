package com.example.core.response

import com.example.memory.models.MemoryRecord

sealed interface MemoryOperation {
    data class Store(val record: MemoryRecord) : MemoryOperation
    data class Update(
        val targetId: String? = null,
        val record: MemoryRecord,
        val previousContent: String? = null
    ) : MemoryOperation
    data class Forget(
        val targetId: String? = null,
        val keywordOrContent: String? = null,
        val reason: String = ""
    ) : MemoryOperation
    data object ClearAll : MemoryOperation
}
