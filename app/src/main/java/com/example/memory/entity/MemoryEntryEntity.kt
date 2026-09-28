package com.example.memory.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val category: String,
    val title: String,
    val content: String,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): MemoryRecord {
        val cat = try {
            MemoryCategory.valueOf(category)
        } catch (_: Exception) {
            MemoryCategory.CONVERSATION_MEMORY
        }
        return MemoryRecord(
            id = id,
            category = cat,
            title = title,
            content = content,
            confidence = confidence,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(record: MemoryRecord): MemoryEntryEntity = MemoryEntryEntity(
            id = record.id,
            category = record.category.name,
            title = record.title,
            content = record.content,
            confidence = record.confidence,
            timestamp = record.timestamp
        )
    }
}
