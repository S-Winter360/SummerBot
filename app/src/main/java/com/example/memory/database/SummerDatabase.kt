package com.example.memory.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.memory.dao.MemoryDao
import com.example.memory.dao.SettingsDao
import com.example.memory.models.MemoryRecord
import com.example.memory.models.SummerSettings

@Database(
    entities = [MemoryRecord::class, SummerSettings::class],
    version = 2,
    exportSchema = false
)
abstract class SummerDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SummerDatabase? = null

        /**
         * Explicit migration from version 1 to version 2.
         * Adds voice settings columns to `summer_settings` introduced in Phase 0F
         * while preserving all existing user settings and memory records intact.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `summer_settings` ADD COLUMN `voiceProfileId` TEXT NOT NULL DEFAULT 'FEMALE'")
                db.execSQL("ALTER TABLE `summer_settings` ADD COLUMN `speechSpeed` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `summer_settings` ADD COLUMN `speechPitch` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `summer_settings` ADD COLUMN `speechVolume` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `summer_settings` ADD COLUMN `preferredVoiceProvider` TEXT NOT NULL DEFAULT 'SYSTEM_OFFLINE'")
            }
        }

        fun getInstance(context: Context): SummerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SummerDatabase::class.java,
                    "summer_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
