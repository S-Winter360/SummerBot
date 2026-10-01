package com.example.core.response

import com.example.memory.models.MemoryRecord

sealed interface MemoryOperation {
    data class Store(val record: MemoryRecord) : MemoryOperation
    data class Forget(val recordId: String) : MemoryOperation
    data class Update(val record: MemoryRecord) : MemoryOperation
}
