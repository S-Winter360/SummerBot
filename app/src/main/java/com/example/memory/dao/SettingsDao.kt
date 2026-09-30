package com.example.memory.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.memory.models.SummerSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SummerSettings)

    @Query("SELECT * FROM summer_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SummerSettings?>

    @Query("SELECT * FROM summer_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SummerSettings?
}
