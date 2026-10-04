package com.example.voice.models

/**
 * Target characteristics for Summer's voice personalities.
 * Expresses the intended acoustic and conversational qualities for future neural engines,
 * separating desired natural qualities from the raw limitations of legacy TTS.
 */
data class VoiceCharacteristics(
    val warmth: Float = 0.85f,
    val naturalness: Float = 0.90f,
    val expressiveness: Float = 0.75f,
    val clarity: Float = 0.95f,
    val pace: String = "conversational",
    val tone: String = "warm, natural, calm, observant, intelligent",
    val description: String = ""
) {
    companion object {
        val FEMALE_TARGET = VoiceCharacteristics(
            warmth = 0.88f,
            naturalness = 0.92f,
            expressiveness = 0.78f,
            clarity = 0.95f,
            pace = "composed conversational",
            tone = "warm, natural, calm, observant, intelligent, conversational",
            description = "Natural female companion voice: clear, warm, intelligent, conversational, and serene."
        )

        val MALE_TARGET = VoiceCharacteristics(
            warmth = 0.85f,
            naturalness = 0.90f,
            expressiveness = 0.72f,
            clarity = 0.95f,
            pace = "grounded conversational",
            tone = "calm, composed, intelligent, warm, resonant",
            description = "Natural male companion voice: steady, composed, intelligent, clear, and reassuring."
        )
    }
}
