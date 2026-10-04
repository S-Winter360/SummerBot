package com.example.voice.provider

import com.example.voice.models.VoiceProfile
import com.example.voice.models.VoiceProviderType

interface VoiceProvider {
    val providerType: VoiceProviderType
    val isAvailable: Boolean
    val isInitialized: Boolean

    suspend fun initialize(): Boolean
    suspend fun speak(
        text: String,
        profile: VoiceProfile,
        onStarted: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean

    suspend fun stop()
    fun getAvailableEngineVoices(): List<String> = emptyList()
    fun release() {}
}
