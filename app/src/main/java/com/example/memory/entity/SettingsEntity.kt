package com.example.memory.entity

import com.example.memory.models.SummerSettings

data class SettingsEntity(
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
) {
    fun toDomain(): SummerSettings = SummerSettings(
        id = id,
        summerEnabled = summerEnabled,
        voiceInteractionEnabled = voiceInteractionEnabled,
        wakeWordEnabled = wakeWordEnabled,
        speakerRecognitionEnabled = speakerRecognitionEnabled,
        proactiveResponsesEnabled = proactiveResponsesEnabled,
        cameraAccessAllowed = cameraAccessAllowed,
        internetAccessAllowed = internetAccessAllowed,
        personalMemoryEnabled = personalMemoryEnabled,
        learningEnabled = learningEnabled
    )

    companion object {
        fun fromDomain(settings: SummerSettings): SettingsEntity = SettingsEntity(
            id = settings.id,
            summerEnabled = settings.summerEnabled,
            voiceInteractionEnabled = settings.voiceInteractionEnabled,
            wakeWordEnabled = settings.wakeWordEnabled,
            speakerRecognitionEnabled = settings.speakerRecognitionEnabled,
            proactiveResponsesEnabled = settings.proactiveResponsesEnabled,
            cameraAccessAllowed = settings.cameraAccessAllowed,
            internetAccessAllowed = settings.internetAccessAllowed,
            personalMemoryEnabled = settings.personalMemoryEnabled,
            learningEnabled = settings.learningEnabled
        )
    }
}
