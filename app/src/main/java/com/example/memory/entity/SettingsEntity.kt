package com.example.memory.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.memory.models.SummerSettings

@Entity(tableName = "summer_settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val summerEnabled: Boolean = true,
    val voiceInteractionEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val proactiveResponsesEnabled: Boolean = true,
    val internetAccessAllowed: Boolean = false,
    val cameraAccessAllowed: Boolean = false,
    val personalMemoryEnabled: Boolean = true,
    val learningEnabled: Boolean = true,
    val speakerRecognitionEnabled: Boolean = false
) {
    fun toDomain(): SummerSettings = SummerSettings(
        summerEnabled = summerEnabled,
        voiceInteractionEnabled = voiceInteractionEnabled,
        wakeWordEnabled = wakeWordEnabled,
        proactiveResponsesEnabled = proactiveResponsesEnabled,
        internetAccessAllowed = internetAccessAllowed,
        cameraAccessAllowed = cameraAccessAllowed,
        personalMemoryEnabled = personalMemoryEnabled,
        learningEnabled = learningEnabled,
        speakerRecognitionEnabled = speakerRecognitionEnabled
    )

    companion object {
        fun fromDomain(settings: SummerSettings): SettingsEntity = SettingsEntity(
            id = 1,
            summerEnabled = settings.summerEnabled,
            voiceInteractionEnabled = settings.voiceInteractionEnabled,
            wakeWordEnabled = settings.wakeWordEnabled,
            proactiveResponsesEnabled = settings.proactiveResponsesEnabled,
            internetAccessAllowed = settings.internetAccessAllowed,
            cameraAccessAllowed = settings.cameraAccessAllowed,
            personalMemoryEnabled = settings.personalMemoryEnabled,
            learningEnabled = settings.learningEnabled,
            speakerRecognitionEnabled = settings.speakerRecognitionEnabled
        )
    }
}
