package com.example.ai.capability

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface OnDeviceGenAIStatusProvider {
    suspend fun checkStatus(): AIAvailabilityStatus
}

class AndroidOnDeviceGenAIStatusProvider : OnDeviceGenAIStatusProvider {

    companion object {
        private const val TAG = "AndroidGenAIStatus"
    }

    override suspend fun checkStatus(): AIAvailabilityStatus = withContext(Dispatchers.IO) {
        try {
            val generativeModel = Generation.getClient()
            val featureStatus: Int = generativeModel.checkStatus()

            when (featureStatus) {
                FeatureStatus.AVAILABLE -> AIAvailabilityStatus.AVAILABLE
                FeatureStatus.DOWNLOADABLE -> AIAvailabilityStatus.DOWNLOADABLE
                FeatureStatus.DOWNLOADING -> AIAvailabilityStatus.DOWNLOADING
                FeatureStatus.UNAVAILABLE -> AIAvailabilityStatus.UNAVAILABLE
                else -> AIAvailabilityStatus.UNAVAILABLE
            }
        } catch (t: Throwable) {
            try {
                android.util.Log.w(TAG, "Official GenAI Prompt API checkStatus returned exception: ${t.message}")
            } catch (_: Throwable) {
                println("[$TAG] Official GenAI Prompt API checkStatus returned exception: ${t.message}")
            }
            AIAvailabilityStatus.ERROR
        }
    }
}
