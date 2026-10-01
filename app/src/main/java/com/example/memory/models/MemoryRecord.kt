package com.example.memory.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_records")
data class MemoryRecord(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: MemoryCategory = MemoryCategory.PREFERENCE,
    val title: String = "",
    val content: String,
    val source: MemorySource = MemorySource.EXPLICIT_USER,
    val importance: MemoryImportance = MemoryImportance.NORMAL,
    val confidence: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val timestamp: Long = createdAt
)
