package com.example.memory.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summer_settings")
data class SummerSettings(
    @PrimaryKey
    val id: Int = 1,
    val summerEnabled: Boolean = true,
    val voiceInteractionEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val speakerRecognitionEnabled: Boolean = false,
    val proactiveResponsesEnabled: Boolean = false,
    val cameraAccessAllowed: Boolean = false,
    val internetAccessAllowed: Boolean = true,
    val personalMemoryEnabled: Boolean = true,
    val learningEnabled: Boolean = false
)
