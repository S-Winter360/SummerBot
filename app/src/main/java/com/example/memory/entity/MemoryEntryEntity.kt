package com.example.memory.entity

import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord

data class MemoryEntryEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
) {
    fun toDomain(): MemoryRecord = MemoryRecord(
        id = id,
        category = try { MemoryCategory.valueOf(category) } catch (_: Exception) { MemoryCategory.CONVERSATION },
        title = title,
        content = content,
        timestamp = timestamp,
        confidence = confidence
    )

    companion object {
        fun fromDomain(record: MemoryRecord): MemoryEntryEntity = MemoryEntryEntity(
            id = record.id,
            category = record.category.name,
            title = record.title,
            content = record.content,
            timestamp = record.timestamp,
            confidence = record.confidence
        )
    }
}
