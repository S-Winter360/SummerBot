package com.example.memory.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.memory.dao.MemoryDao
import com.example.memory.dao.SettingsDao
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings

@Database(
    entities = [MemoryRecord::class, SummerSettings::class],
    version = 1,
    exportSchema = false
)
abstract class SummerDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SummerDatabase? = null

        fun getInstance(context: Context): SummerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SummerDatabase::class.java,
                    "summer_database.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
