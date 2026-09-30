package com.example.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(record: MemoryRecord)

    @Query("SELECT * FROM memory_records ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryRecord>>

    @Query("SELECT * FROM memory_records WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>>

    @Query("DELETE FROM memory_records WHERE id = :id")
    suspend fun deleteMemoryById(id: String)
}
