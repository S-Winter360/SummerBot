package com.example.ai.capability

import com.example.ai.AIEngine
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse

class OnDeviceGenAIProvider(
    private val statusProvider: OnDeviceGenAIStatusProvider = AndroidOnDeviceGenAIStatusProvider(),
    initialStatus: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING
) : AIEngine {

    var metadata: AIModelMetadata = AIModelMetadata(
        provider = AIProviderType.ON_DEVICE_GENAI,
        modelIdentifier = "gemini-nano-prompt-api",
        displayName = "Gemini Nano (ML Kit Prompt API)",
        modelVersion = "1.0.0-beta4",
        availabilityStatus = initialStatus,
        supportedCapabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CHAT,
            AICapability.STRUCTURED_OUTPUT,
            AICapability.SUMMARIZATION
        ),
        offlineCapable = true,
        multimodalSupport = true
    )
        private set

    override val modelInfo: AIModelInfo
        get() = AIModelInfo(
            name = metadata.displayName,
            version = metadata.modelVersion ?: "unknown",
            isLocalOffline = metadata.offlineCapable,
            description = "On-device Gemini Nano via ML Kit GenAI Prompt API."
        )

    override val isReady: Boolean
        get() = metadata.availabilityStatus == AIAvailabilityStatus.AVAILABLE

    suspend fun checkAvailability(): AIAvailabilityStatus {
        val status = statusProvider.checkStatus()
        updateAvailability(status)
        return status
    }

    fun updateAvailability(status: AIAvailabilityStatus) {
        metadata = metadata.copy(
            availabilityStatus = status,
            lastCheckedTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun classifyIntent(input: String): SummerIntent {
        return SummerIntent.Unknown(input, confidence = 0.0f)
    }

    override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult {
        val responseText = if (isReady) {
            "On-device GenAI engine is detected as available. Inference execution is deferred to Phase 0D."
        } else {
            "On-device GenAI provider is currently ${metadata.availabilityStatus.label.lowercase()}. Routing to deterministic local reasoning."
        }

        return AIResult(
            intent = SummerIntent.Unknown(interaction.userInput),
            response = SummerResponse(
                text = responseText,
                type = if (isReady) ResponseType.INFORMATION else ResponseType.ERROR,
                confidence = 0.0f,
                source = metadata.displayName
            ),
            confidence = 0.0f
        )
    }

    override suspend fun evaluateIntent(input: String): RecognizedIntent {
        return RecognizedIntent.GeneralConversation(input)
    }

    override suspend fun processQuery(request: AIRequest): AIResponse {
        return AIResponse(
            text = "On-device GenAI query processing deferred to Phase 0D.",
            modelUsed = metadata.displayName,
            recognizedIntent = RecognizedIntent.GeneralConversation(request.query)
        )
    }
}
