package com.example.security

/**
 * Defines sensitive device and system capabilities guarded by Summer's security boundary.
 * Future AI agents must never bypass this capability authorization layer.
 */
enum class Capability(val identifier: String, val title: String, val riskLevel: RiskLevel) {
    MICROPHONE("cap.microphone", "Audio & Microphone Access", RiskLevel.HIGH),
    CAMERA("cap.camera", "Vision & Camera Access", RiskLevel.HIGH),
    INTERNET("cap.internet", "External Network Connectivity", RiskLevel.MEDIUM),
    DEVICE_SETTINGS("cap.device_settings", "Device Configuration Modification", RiskLevel.HIGH),
    LOCAL_MEMORY_WRITE("cap.memory.write", "Persistent Memory Mutation", RiskLevel.LOW),
    LOCAL_MEMORY_READ("cap.memory.read", "Personal Data Retrieval", RiskLevel.MEDIUM),
    SPEAKER_RECOGNITION("cap.biometrics.voice", "Voiceprint Matching", RiskLevel.HIGH),
    SYSTEM_AUTOMATION("cap.automation", "System Task Automation", RiskLevel.CRITICAL)
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
