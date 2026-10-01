package com.example.ai.capability

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface defining access to on-device generative model capabilities.
 * Decouples the concrete ML Kit Prompt API SDK from business logic and allows deterministic test injection.
 */
interface GeminiClient {
    suspend fun checkStatus(): AIAvailabilityStatus
    suspend fun generateContent(prompt: String): String?
    suspend fun warmup()
}

/**
 * Production implementation using Google's official ML Kit GenAI Prompt API (com.google.mlkit:genai-prompt:1.0.0-beta4).
 * Reuses the GenerativeModel instance across calls and executes inference safely on Dispatchers.IO.
 */
class AndroidMLKitGeminiClient : GeminiClient {

    companion object {
        private const val TAG = "MLKitGeminiClient"
    }

    private var generativeModel: GenerativeModel? = null

    private fun getOrCreateModel(): GenerativeModel {
        var model = generativeModel
        if (model == null) {
            model = Generation.getClient()
            generativeModel = model
        }
        return model
    }

    override suspend fun checkStatus(): AIAvailabilityStatus = withContext(Dispatchers.IO) {
        try {
            val model = getOrCreateModel()
            val featureStatus: Int = model.checkStatus()
            when (featureStatus) {
                FeatureStatus.AVAILABLE -> AIAvailabilityStatus.AVAILABLE
                FeatureStatus.DOWNLOADABLE -> AIAvailabilityStatus.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> AIAvailabilityStatus.DOWNLOADING
                FeatureStatus.UNAVAILABLE -> AIAvailabilityStatus.UNAVAILABLE
                else -> AIAvailabilityStatus.UNAVAILABLE
            }
        } catch (t: Throwable) {
            logWarn("checkStatus failure: ${t.message}")
            AIAvailabilityStatus.ERROR
        }
    }

    override suspend fun generateContent(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val model = getOrCreateModel()
            val response = model.generateContent(prompt)
            val text = response.candidates.firstOrNull()?.text
            text?.trim()
        } catch (t: Throwable) {
            logWarn("generateContent error: ${t.message}")
            null
        }
    }

    override suspend fun warmup() = withContext(Dispatchers.IO) {
        try {
            val model = getOrCreateModel()
            model.warmup()
        } catch (t: Throwable) {
            logWarn("warmup failed (ignorable): ${t.message}")
        }
    }

    private fun logWarn(msg: String) {
        try {
            android.util.Log.w(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }
}
