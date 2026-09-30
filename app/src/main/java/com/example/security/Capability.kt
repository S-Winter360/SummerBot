package com.example.security

enum class Capability(val title: String, val requiresUserConfirmation: Boolean) {
    INTERNET("Internet Connectivity", false),
    MICROPHONE("Audio Capture", false),
    CAMERA("Visual Input", true),
    STORAGE("Local Storage Read/Write", false),
    SYSTEM_SETTINGS("Modify Device Settings", true),
    SPEECH_SYNTHESIS("Audio Voice Output", false)
}
