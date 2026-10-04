package com.example.voice

import com.example.voice.models.SpeechState
import com.example.voice.models.VoiceDiagnostics
import com.example.voice.models.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StubVoiceEngine : VoiceEngine {
    override val isAvailable: Boolean = false
    override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val speechState: StateFlow<SpeechState> = MutableStateFlow(SpeechState.IDLE).asStateFlow()
    override val currentProfile: StateFlow<VoiceProfile?> = MutableStateFlow(VoiceProfile.SUMMER_FEMALE).asStateFlow()
    override val availableProfiles: StateFlow<List<VoiceProfile>> = MutableStateFlow(VoiceProfile.BUILT_IN_PROFILES).asStateFlow()
    override val diagnostics: StateFlow<VoiceDiagnostics> = MutableStateFlow(VoiceDiagnostics(systemActive = false)).asStateFlow()

    override suspend fun speak(text: String, profile: VoiceProfile?): Boolean = false
    override suspend fun stop() {}
    override suspend fun startListening(): Boolean = false
    override suspend fun stopListening() {}
}
