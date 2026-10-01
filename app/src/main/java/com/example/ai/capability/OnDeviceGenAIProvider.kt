package com.example.ai.capability

import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent

/**
 * OnDeviceGenAIProvider delegates cognitive operations to [OnDeviceGeminiNanoAIEngine],
 * maintaining backwards compatibility with Phase 0C-R1 contracts.
 */
class OnDeviceGenAIProvider(
    private val statusProvider: OnDeviceGenAIStatusProvider = AndroidOnDeviceGenAIStatusProvider(),
    private val geminiClient: GeminiClient = AndroidMLKitGeminiClient(),
    private val fallbackEngine: OfflineLocalAIEngine = OfflineLocalAIEngine(),
    initialStatus: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING
) : AIEngine {

    val geminiNanoEngine = OnDeviceGeminiNanoAIEngine(
        client = geminiClient,
        fallbackEngine = fallbackEngine,
        initialStatus = initialStatus
    )

    val metadata: AIModelMetadata
        get() = geminiNanoEngine.metadata

    override val modelInfo: AIModelInfo
        get() = geminiNanoEngine.modelInfo

    override val isReady: Boolean
        get() = geminiNanoEngine.isReady

    suspend fun checkAvailability(): AIAvailabilityStatus {
        val status = statusProvider.checkStatus()
        geminiNanoEngine.updateAvailability(status)
        return status
    }

    fun updateAvailability(status: AIAvailabilityStatus) {
        geminiNanoEngine.updateAvailability(status)
    }

    suspend fun warmup() {
        geminiNanoEngine.warmup()
    }

    override suspend fun classifyIntent(input: String): SummerIntent {
        return geminiNanoEngine.classifyIntent(input)
    }

    override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult {
        return geminiNanoEngine.process(context, interaction)
    }

    override suspend fun evaluateIntent(input: String): RecognizedIntent {
        return geminiNanoEngine.evaluateIntent(input)
    }

    override suspend fun processQuery(request: AIRequest): AIResponse {
        return geminiNanoEngine.processQuery(request)
    }
}
