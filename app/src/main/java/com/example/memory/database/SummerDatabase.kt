package com.example.memory.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.memory.dao.MemoryDao
import com.example.memory.dao.SettingsDao
import com.example.memory.entity.MemoryEntryEntity
import com.example.memory.entity.SettingsEntity

@Database(
    entities = [
        SettingsEntity::class,
        MemoryEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SummerDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: SummerDatabase? = null

        fun getInstance(context: Context): SummerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SummerDatabase::class.java,
                    "summer_winter.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
