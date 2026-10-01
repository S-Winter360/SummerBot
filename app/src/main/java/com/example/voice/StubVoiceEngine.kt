package com.example.voice

class StubVoiceEngine : VoiceEngine {
    override val isAvailable: Boolean = false
    override suspend fun startListening(): Boolean = false
    override suspend fun stopListening() {}
}
