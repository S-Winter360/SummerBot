package com.example.ai.capability

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.example.network.NetworkInformationProvider
import com.example.network.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDeviceAICapabilityDetector(
    private val context: Context,
    private val networkProvider: NetworkInformationProvider? = null,
    private val genAIStatusProvider: OnDeviceGenAIStatusProvider = AndroidOnDeviceGenAIStatusProvider()
) : DeviceAICapabilityDetector {

    companion object {
        private const val TAG = "AICapabilityDetector"
        private const val AICORE_PACKAGE = "com.google.android.aicore"
    }

    override suspend fun detect(): DeviceAIProfile = withContext(Dispatchers.IO) {
        logInfo("Starting device AI capability detection...")

        val apiLevel = try { Build.VERSION.SDK_INT } catch (_: Throwable) { 0 }
        val manufacturer = try { Build.MANUFACTURER ?: "Unknown" } catch (_: Throwable) { "Unknown" }
        val model = try { Build.MODEL ?: "Unknown" } catch (_: Throwable) { "Unknown" }

        // Memory diagnostics
        var totalMem: Long? = null
        var availMem: Long? = null
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                totalMem = memInfo.totalMem
                availMem = memInfo.availMem
            }
        } catch (t: Throwable) {
            logError("Failed to query ActivityManager memory info", t)
        }

        // Storage diagnostics
        var totalStorage: Long? = null
        var availStorage: Long? = null
        try {
            val dataPath = context.filesDir.absolutePath
            val stat = StatFs(dataPath)
            totalStorage = stat.totalBytes
            availStorage = stat.availableBytes
        } catch (t: Throwable) {
            logError("Failed to query filesystem storage info", t)
        }

        // Network check
        val isNetworkAvailable = try {
            val state = networkProvider?.getCurrentState()
            state != null && state != NetworkState.OFFLINE
        } catch (_: Throwable) {
            false
        }

        // Supplementary diagnostic: check AICore package presence
        val isAiCoreInstalled = try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo(AICORE_PACKAGE, 0)
            true
        } catch (_: Throwable) {
            false
        }

        // Authoritative on-device GenAI readiness from official ML Kit GenAI Prompt API
        val genAiStatus = try {
            genAIStatusProvider.checkStatus()
        } catch (t: Throwable) {
            logError("On-device GenAI capability detection threw exception", t)
            AIAvailabilityStatus.ERROR
        }
        logInfo("Official GenAI Prompt API status: ${genAiStatus.name} (AICore installed=$isAiCoreInstalled)")

        val detectedProviders = mutableListOf<AIProviderType>()
        detectedProviders.add(AIProviderType.DETERMINISTIC_LOCAL)

        if (genAiStatus == AIAvailabilityStatus.AVAILABLE ||
            genAiStatus == AIAvailabilityStatus.DOWNLOADABLE ||
            genAiStatus == AIAvailabilityStatus.DOWNLOADING
        ) {
            detectedProviders.add(0, AIProviderType.ON_DEVICE_GENAI)
        }

        // Only AVAILABLE activates On-Device GenAI as the recommended provider; all others fallback to Deterministic Local
        val recommendedProvider = if (genAiStatus == AIAvailabilityStatus.AVAILABLE) {
            AIProviderType.ON_DEVICE_GENAI
        } else {
            AIProviderType.DETERMINISTIC_LOCAL
        }

        val profile = DeviceAIProfile(
            apiLevel = apiLevel,
            manufacturer = manufacturer,
            model = model,
            totalMemoryBytes = totalMem,
            availableMemoryBytes = availMem,
            totalStorageBytes = totalStorage,
            availableStorageBytes = availStorage,
            isNetworkAvailable = isNetworkAvailable,
            onDeviceGenAIAvailability = genAiStatus,
            isAiCoreInstalled = isAiCoreInstalled,
            detectedProviders = detectedProviders,
            recommendedProvider = recommendedProvider,
            checkTimestamp = System.currentTimeMillis()
        )

        logInfo("Device AI capability detection complete. Recommended provider: ${recommendedProvider.name}")
        profile
    }

    private fun logInfo(msg: String) {
        try {
            android.util.Log.i(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun logError(msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(TAG, msg, tr)
        } catch (_: Throwable) {
            System.err.println("[$TAG] $msg: ${tr?.message}")
        }
    }
}
