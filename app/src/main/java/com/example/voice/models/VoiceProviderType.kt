package com.example.voice.models

enum class VoiceProviderType(val displayName: String) {
    NATURAL_LOCAL("Natural Local Neural Voice"),
    SYSTEM_OFFLINE("System Offline TTS (Android TextToSpeech)"),
    OPTIONAL_ONLINE("Optional Online Provider (Future)")
}
