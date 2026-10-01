package com.example.memory.entity

import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryImportance
import com.example.memory.models.MemoryRecord
import com.example.memory.models.MemorySource

data class MemoryEntryEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String,
    val title: String,
    val content: String,
    val source: String = MemorySource.EXPLICIT_USER.name,
    val importance: String = MemoryImportance.NORMAL.name,
    val confidence: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val timestamp: Long = createdAt
) {
    fun toDomain(): MemoryRecord = MemoryRecord(
        id = id,
        category = try { MemoryCategory.valueOf(category) } catch (_: Exception) { MemoryCategory.PREFERENCE },
        title = title,
        content = content,
        source = try { MemorySource.valueOf(source) } catch (_: Exception) { MemorySource.EXPLICIT_USER },
        importance = try { MemoryImportance.valueOf(importance) } catch (_: Exception) { MemoryImportance.NORMAL },
        confidence = confidence.coerceIn(0.0f, 1.0f),
        createdAt = createdAt,
        updatedAt = updatedAt,
        accessCount = accessCount,
        lastAccessedAt = lastAccessedAt,
        isActive = isActive,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(record: MemoryRecord): MemoryEntryEntity = MemoryEntryEntity(
            id = record.id,
            category = record.category.name,
            title = record.title,
            content = record.content,
            source = record.source.name,
            importance = record.importance.name,
            confidence = record.confidence,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            accessCount = record.accessCount,
            lastAccessedAt = record.lastAccessedAt,
            isActive = record.isActive,
            timestamp = record.timestamp
        )
    }
}
