package com.example.voice

import com.example.voice.models.SpeechState
import com.example.voice.models.VoiceDiagnostics
import com.example.voice.models.VoiceProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Core speech generation and audio playback abstraction for Summer.
 * Decoupled from any single platform technology or vendor.
 */
interface VoiceEngine {
    val isAvailable: Boolean
    val isSpeaking: StateFlow<Boolean>
    val speechState: StateFlow<SpeechState>
    val currentProfile: StateFlow<VoiceProfile?>
    val availableProfiles: StateFlow<List<VoiceProfile>>
    val diagnostics: StateFlow<VoiceDiagnostics>

    suspend fun speak(text: String, profile: VoiceProfile? = null): Boolean
    suspend fun stop()
    suspend fun pause() {}
    suspend fun resume() {}

    // Backwards compatibility with Phase 0A/0B stubs
    suspend fun startListening(): Boolean = false
    suspend fun stopListening() {}
}
