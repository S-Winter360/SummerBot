package com.example.voice.models

/**
 * Diagnostic metadata for Summer's voice system.
 * Transparently reports the active provider (e.g. System Offline TTS fallback vs Natural Local),
 * resolved engine voice, and available profiles without exposing audio data or private content.
 */
data class VoiceDiagnostics(
    val systemActive: Boolean = true,
    val activeProvider: VoiceProviderType = VoiceProviderType.SYSTEM_OFFLINE,
    val selectedProfile: VoiceProfileId = VoiceProfileId.FEMALE,
    val resolvedEngineVoice: String? = null,
    val isOfflineCapable: Boolean = true,
    val isNaturalNeuralAvailable: Boolean = false,
    val availableProfiles: List<VoiceProfileId> = listOf(VoiceProfileId.FEMALE, VoiceProfileId.MALE),
    val speechState: SpeechState = SpeechState.IDLE,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val statusMessage: String = "Ready (System Offline Fallback)"
)
