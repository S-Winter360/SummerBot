package com.example.memory.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_records")
data class MemoryRecord(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: MemoryCategory,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)
