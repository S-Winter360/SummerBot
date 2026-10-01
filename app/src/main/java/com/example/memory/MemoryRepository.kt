package com.example.memory

import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeSettings(): Flow<SummerSettings>
    suspend fun getSettings(): SummerSettings
    suspend fun updateSettings(settings: SummerSettings)
    suspend fun recordMemory(record: MemoryRecord)
    suspend fun updateMemory(record: MemoryRecord)
    suspend fun getActiveMemories(): List<MemoryRecord>
    fun observeMemories(): Flow<List<MemoryRecord>>
    fun observeMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>>
    suspend fun deleteMemory(id: String)
    suspend fun forgetMemory(targetId: String?, keyword: String?): Boolean
    suspend fun clearAllMemories()
    suspend fun updateAccessMetadata(id: String, count: Int, timestamp: Long)
}
