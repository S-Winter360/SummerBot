package com.example.memory

import com.example.memory.dao.MemoryDao
import com.example.memory.dao.SettingsDao
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

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

    override suspend fun updateMemory(record: MemoryRecord) =
        memoryDao.updateMemory(record.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun getActiveMemories(): List<MemoryRecord> =
        memoryDao.getActiveMemories()

    override fun observeMemories(): Flow<List<MemoryRecord>> =
        memoryDao.getAllMemories()

    override fun observeMemoriesByCategory(category: MemoryCategory): Flow<List<MemoryRecord>> =
        memoryDao.getMemoriesByCategory(category)

    override suspend fun deleteMemory(id: String) =
        memoryDao.deleteMemoryById(id)

    override suspend fun forgetMemory(targetId: String?, keyword: String?): Boolean {
        if (!targetId.isNullOrBlank()) {
            memoryDao.deleteMemoryById(targetId)
            return true
        }
        if (!keyword.isNullOrBlank()) {
            val normalizedKeyword = keyword.trim().lowercase(Locale.ROOT)
            val memories = memoryDao.getActiveMemories()
            val matches = memories.filter {
                it.content.lowercase(Locale.ROOT).contains(normalizedKeyword) ||
                    it.title.lowercase(Locale.ROOT).contains(normalizedKeyword)
            }
            if (matches.isNotEmpty()) {
                // Delete specific matched memories
                for (match in matches) {
                    memoryDao.deleteMemoryById(match.id)
                }
                return true
            }
        }
        return false
    }

    override suspend fun clearAllMemories() =
        memoryDao.clearAllMemories()

    override suspend fun updateAccessMetadata(id: String, count: Int, timestamp: Long) =
        memoryDao.updateAccessMetadata(id, count, timestamp)
}
