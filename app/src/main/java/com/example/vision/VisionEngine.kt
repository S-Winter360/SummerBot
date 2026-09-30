package com.example.vision

interface VisionEngine {
    val isReady: Boolean
    suspend fun processFrame(frameBytes: ByteArray): String
}
