package com.example.ai.capability

interface DeviceAICapabilityDetector {
    suspend fun detect(): DeviceAIProfile
}
