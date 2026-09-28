package com.example.memory

import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import kotlinx.coroutines.flow.Flow

/**
 * Architectural abstraction for all persistent memory operations in Summer.
 * Decouples the storage engine (Room, SQLite, or future vector store) from business logic.
 */
interface MemoryRepository {

    /**
     * Observes the persistent configuration settings for Summer.
     */
    fun observeSettings(): Flow<SummerSettings>

    /**
     * Retrieves the current settings snapshot synchronously.
     */
    suspend fun getSettings(): SummerSettings

    /**
     * Persists updated settings.
     */
    suspend fun updateSettings(settings: SummerSettings)

    /**
     * Observes recent memories (conversations, facts, preferences).
     */
    fun observeRecentMemories(limit: Int = 10): Flow<List<MemoryRecord>>

    /**
     * Stores a new memory record.
     */
    suspend fun recordMemory(record: MemoryRecord): Long

    /**
     * Queries memories matching keywords.
     */
    suspend fun searchMemories(query: String): List<MemoryRecord>
}
