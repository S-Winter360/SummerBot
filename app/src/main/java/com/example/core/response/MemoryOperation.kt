package com.example.core.response

import com.example.memory.models.MemoryRecord

/**
 * Operations requested by the decision pipeline to mutate local memory.
 */
sealed interface MemoryOperation {
    data class Store(val record: MemoryRecord) : MemoryOperation
    data class Forget(val memoryId: Long) : MemoryOperation
    data class Update(val record: MemoryRecord) : MemoryOperation
}
