package com.example.memory

import com.example.memory.dao.MemoryDao
import com.example.memory.dao.SettingsDao
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MemoryRepository {
    fun observeSettings(): Flow<SummerSettings>
    suspend fun getSettings(): SummerSettings
    suspend fun updateSettings(settings: SummerSettings)
    suspend fun recordMemory(record: MemoryRecord)
    fun observeMemories(): Flow<List<MemoryRecord>>
    fun observeMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>>
    suspend fun deleteMemory(id: String)
}

class LocalMemoryRepository(
    private val settingsDao: SettingsDao,
    private val memoryDao: MemoryDao
) : MemoryRepository {
    override fun observeSettings(): Flow<SummerSettings> =
        settingsDao.getSettingsFlow().map { it ?: SummerSettings() }

    override suspend fun getSettings(): SummerSettings =
        settingsDao.getSettings() ?: SummerSettings()

    override suspend fun updateSettings(settings: SummerSettings) =
        settingsDao.saveSettings(settings)

    override suspend fun recordMemory(record: MemoryRecord) =
        memoryDao.insertMemory(record)

    override fun observeMemories(): Flow<List<MemoryRecord>> =
        memoryDao.getAllMemories()

    override fun observeMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>> =
        memoryDao.getMemoriesByCategory(category)

    override suspend fun deleteMemory(id: String) =
        memoryDao.deleteMemoryById(id)
}
