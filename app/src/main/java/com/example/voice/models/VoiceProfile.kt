package com.example.voice.models

import java.util.Locale

/**
 * First-class representation of Summer's voice profile.
 * Decoupled from any single engine, allowing a future neural voice engine
 * or system fallback TTS to resolve appropriate voice parameters.
 */
data class VoiceProfile(
    val id: VoiceProfileId,
    val displayName: String,
    val gender: VoiceGender,
    val locale: Locale = Locale.US,
    val providerPreference: VoiceProviderType = VoiceProviderType.NATURAL_LOCAL,
    val engineVoiceId: String? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val targetCharacteristics: VoiceCharacteristics = VoiceCharacteristics(),
    val isAvailable: Boolean = true,
    val isBuiltIn: Boolean = true
) {
    companion object {
        val SUMMER_FEMALE = VoiceProfile(
            id = VoiceProfileId.FEMALE,
            displayName = VoiceProfileId.FEMALE.displayName,
            gender = VoiceGender.FEMALE,
            locale = Locale.US,
            providerPreference = VoiceProviderType.NATURAL_LOCAL,
            engineVoiceId = null,
            speechRate = 1.0f,
            pitch = 1.0f,
            volume = 1.0f,
            targetCharacteristics = VoiceCharacteristics.FEMALE_TARGET,
            isAvailable = true,
            isBuiltIn = true
        )

        val SUMMER_MALE = VoiceProfile(
            id = VoiceProfileId.MALE,
            displayName = VoiceProfileId.MALE.displayName,
            gender = VoiceGender.MALE,
            locale = Locale.US,
            providerPreference = VoiceProviderType.NATURAL_LOCAL,
            engineVoiceId = null,
            speechRate = 1.0f,
            pitch = 0.95f,
            volume = 1.0f,
            targetCharacteristics = VoiceCharacteristics.MALE_TARGET,
            isAvailable = true,
            isBuiltIn = true
        )

        val BUILT_IN_PROFILES: List<VoiceProfile> = listOf(SUMMER_FEMALE, SUMMER_MALE)

        fun fromId(id: VoiceProfileId): VoiceProfile = when (id) {
            VoiceProfileId.FEMALE -> SUMMER_FEMALE
            VoiceProfileId.MALE -> SUMMER_MALE
        }

        fun fromString(idString: String?): VoiceProfile = when (idString?.uppercase()) {
            "MALE", "SUMMER_MALE" -> SUMMER_MALE
            else -> SUMMER_FEMALE
        }
    }
}
