package com.example.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(record: MemoryRecord)

    @Update
    suspend fun updateMemory(record: MemoryRecord)

    @Query("SELECT * FROM memory_records WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryRecord>>

    @Query("SELECT * FROM memory_records WHERE isActive = 1 ORDER BY updatedAt DESC")
    suspend fun getActiveMemories(): List<MemoryRecord>

    @Query("SELECT * FROM memory_records WHERE category = :category AND isActive = 1 ORDER BY updatedAt DESC")
    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>>

    @Query("DELETE FROM memory_records WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM memory_records")
    suspend fun clearAllMemories()

    @Query("UPDATE memory_records SET accessCount = :count, lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateAccessMetadata(id: String, count: Int, timestamp: Long)
}
