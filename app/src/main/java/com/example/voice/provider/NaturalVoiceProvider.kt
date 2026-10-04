package com.example.voice.provider

import com.example.voice.models.VoiceProfile
import com.example.voice.models.VoiceProviderType

/**
 * Interface and architectural boundary for Summer's future on-device natural neural voice engine.
 * Truth in reporting: In this phase, returns isAvailable = false without synthetic claims or fake audio.
 */
interface NaturalVoiceProvider : VoiceProvider {
    override val providerType: VoiceProviderType get() = VoiceProviderType.NATURAL_LOCAL
    val supportedProfileIds: List<String> get() = emptyList()
}

class DefaultNaturalVoiceProvider : NaturalVoiceProvider {
    override val isAvailable: Boolean = false
    override val isInitialized: Boolean = true

    override suspend fun initialize(): Boolean {
        // Reserved for future local neural voice model initialization
        return false
    }

    override suspend fun speak(
        text: String,
        profile: VoiceProfile,
        onStarted: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        onError("Natural local neural voice engine is not yet installed on this device.")
        return false
    }

    override suspend fun stop() {}
    override fun release() {}
}
