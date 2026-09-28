package com.example.memory

import com.example.memory.dao.MemoryDao
import com.example.memory.dao.SettingsDao
import com.example.memory.entity.MemoryEntryEntity
import com.example.memory.entity.SettingsEntity
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Production implementation of MemoryRepository backed by local Room database.
 * Completely offline and zero-cloud.
 */
class LocalMemoryRepository(
    private val settingsDao: SettingsDao,
    private val memoryDao: MemoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MemoryRepository {

    override fun observeSettings(): Flow<SummerSettings> {
        return settingsDao.observeSettings().map { entity ->
            entity?.toDomain() ?: SummerSettings()
        }
    }

    override suspend fun getSettings(): SummerSettings = withContext(ioDispatcher) {
        settingsDao.getSettingsDirect()?.toDomain() ?: run {
            val defaultSettings = SummerSettings()
            settingsDao.upsertSettings(SettingsEntity.fromDomain(defaultSettings))
            defaultSettings
        }
    }

    override suspend fun updateSettings(settings: SummerSettings) = withContext(ioDispatcher) {
        settingsDao.upsertSettings(SettingsEntity.fromDomain(settings))
    }

    override fun observeRecentMemories(limit: Int): Flow<List<MemoryRecord>> {
        return memoryDao.observeRecentMemories(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordMemory(record: MemoryRecord): Long = withContext(ioDispatcher) {
        memoryDao.insertMemory(MemoryEntryEntity.fromDomain(record))
    }

    override suspend fun searchMemories(query: String): List<MemoryRecord> = withContext(ioDispatcher) {
        memoryDao.searchMemories(query).map { it.toDomain() }
    }
}
