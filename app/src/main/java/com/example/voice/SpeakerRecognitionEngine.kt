package com.example.voice

interface SpeakerRecognitionEngine {
    val isEnrolled: Boolean
    suspend fun verifySpeaker(audioSample: ByteArray): Boolean
}
