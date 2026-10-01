package com.example.voice

interface VoiceEngine {
    val isAvailable: Boolean
    suspend fun startListening(): Boolean
    suspend fun stopListening()
}
