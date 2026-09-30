package com.example.voice

interface VoiceEngine {
    val isAvailable: Boolean
    suspend fun startListening(): Boolean
    suspend fun stopListening()
}

interface SpeakerRecognitionEngine {
    val isEnrolled: Boolean
    suspend fun verifySpeaker(audioSample: ByteArray): Boolean
}
