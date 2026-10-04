package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.memory.database.SummerDatabase
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryImportance
import com.example.memory.models.MemorySource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 0F-R1: Verifies explicit Room migration (1 -> 2) without destructive data loss.
 * Confirms that user memories, user settings, and new voice columns are preserved and initialized.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    private val dbName = "migration_verification_test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration1To2PreservesSettingsAndMemories() = runBlocking {
        // Step A: Create a version-1 database using raw SQLite
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `memory_records` (
                            `id` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `source` TEXT NOT NULL,
                            `importance` TEXT NOT NULL,
                            `confidence` REAL NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            `accessCount` INTEGER NOT NULL,
                            `lastAccessedAt` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `summer_settings` (
                            `id` INTEGER NOT NULL,
                            `summerEnabled` INTEGER NOT NULL,
                            `voiceInteractionEnabled` INTEGER NOT NULL,
                            `wakeWordEnabled` INTEGER NOT NULL,
                            `speakerRecognitionEnabled` INTEGER NOT NULL,
                            `proactiveResponsesEnabled` INTEGER NOT NULL,
                            `cameraAccessAllowed` INTEGER NOT NULL,
                            `internetAccessAllowed` INTEGER NOT NULL,
                            `personalMemoryEnabled` INTEGER NOT NULL,
                            `learningEnabled` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())

                    db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                    db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7195086a32dfdf3ca01941ecc7aaa60a')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v1Helper = factory.create(config)
        val v1Db = v1Helper.writableDatabase

        // Step B: Insert representative existing settings into v1
        v1Db.execSQL("""
            INSERT INTO `summer_settings` (
                `id`, `summerEnabled`, `voiceInteractionEnabled`, `wakeWordEnabled`,
                `speakerRecognitionEnabled`, `proactiveResponsesEnabled`, `cameraAccessAllowed`,
                `internetAccessAllowed`, `personalMemoryEnabled`, `learningEnabled`
            ) VALUES (1, 1, 1, 0, 1, 0, 1, 1, 1, 0)
        """.trimIndent())

        // Step C: Insert representative existing memory into v1
        v1Db.execSQL("""
            INSERT INTO `memory_records` (
                `id`, `category`, `title`, `content`, `source`, `importance`,
                `confidence`, `createdAt`, `updatedAt`, `accessCount`, `lastAccessedAt`,
                `isActive`, `timestamp`
            ) VALUES (
                'mem_mig_001', 'PREFERENCE', 'User Coffee Choice', 'Prefers dark roast with oat milk',
                'EXPLICIT_USER', 'HIGH', 1.0, 1710000000000, 1710000000000, 5, 1710000060000, 1, 1710000000000
            )
        """.trimIndent())

        v1Db.close()
        v1Helper.close()

        // Step D: Run migration 1 -> 2 by opening with Room configured with MIGRATION_1_2
        val roomDb = Room.databaseBuilder(
            context,
            SummerDatabase::class.java,
            dbName
        )
            .addMigrations(SummerDatabase.MIGRATION_1_2)
            .build()

        // Trigger database opening and schema validation
        val settings = roomDb.settingsDao().getSettings()

        // Step E: Verify existing settings remain intact
        assertNotNull(settings)
        assertEquals(1, settings!!.id)
        assertTrue(settings.summerEnabled)
        assertTrue(settings.voiceInteractionEnabled)
        assertFalse(settings.wakeWordEnabled)
        assertTrue(settings.speakerRecognitionEnabled)
        assertFalse(settings.proactiveResponsesEnabled)
        assertTrue(settings.cameraAccessAllowed)
        assertTrue(settings.internetAccessAllowed)
        assertTrue(settings.personalMemoryEnabled)
        assertFalse(settings.learningEnabled)

        // Step F: Verify existing memory remains intact
        val memories = roomDb.memoryDao().getActiveMemories()
        val memory = memories.firstOrNull { it.id == "mem_mig_001" }
        assertNotNull(memory)
        assertEquals("mem_mig_001", memory!!.id)
        assertEquals(MemoryCategory.PREFERENCE, memory.category)
        assertEquals("User Coffee Choice", memory.title)
        assertEquals("Prefers dark roast with oat milk", memory.content)
        assertEquals(MemorySource.EXPLICIT_USER, memory.source)
        assertEquals(MemoryImportance.HIGH, memory.importance)
        assertEquals(1.0f, memory.confidence, 0.001f)
        assertEquals(5, memory.accessCount)
        assertTrue(memory.isActive)

        // Step G & H: Verify new voice fields exist and contain correct defaults
        assertEquals("FEMALE", settings.voiceProfileId)
        assertEquals(1.0f, settings.speechSpeed, 0.001f)
        assertEquals(1.0f, settings.speechPitch, 0.001f)
        assertEquals(1.0f, settings.speechVolume, 0.001f)
        assertEquals("SYSTEM_OFFLINE", settings.preferredVoiceProvider)

        // Step I & J: Verify Room schema validation succeeded and database is at version 2
        assertTrue(roomDb.isOpen)
        assertEquals(2, roomDb.openHelper.readableDatabase.version)

        roomDb.close()
    }
}
