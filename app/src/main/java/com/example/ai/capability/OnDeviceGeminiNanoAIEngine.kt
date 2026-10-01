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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-Device Generative AI Engine for Summer powered by Google's official ML Kit GenAI Prompt API
 * (com.google.mlkit:genai-prompt:1.0.0-beta4 / Gemini Nano).
 *
 * Guaranteed Safety & Fallback:
 * - Executes local inference ONLY when [checkAvailability] or metadata indicates [AIAvailabilityStatus.AVAILABLE].
 * - Automatically falls back to [OfflineLocalAIEngine] if status is not AVAILABLE, or if generation fails/errors.
 * - Non-blocking prompt generation and parsing on background coroutine dispatchers.
 */
class OnDeviceGeminiNanoAIEngine(
    private val client: GeminiClient = AndroidMLKitGeminiClient(),
    private val fallbackEngine: OfflineLocalAIEngine = OfflineLocalAIEngine(),
    private val promptBuilder: SummerPromptBuilder = SummerPromptBuilder(),
    private val responseParser: GeminiResponseParser = GeminiResponseParser(),
    initialStatus: AIAvailabilityStatus = AIAvailabilityStatus.CHECKING
) : AIEngine {

    companion object {
        private const val TAG = "GeminiNanoAIEngine"
    }

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
            description = "On-device Gemini Nano via official ML Kit GenAI Prompt API."
        )

    override val isReady: Boolean
        get() = metadata.availabilityStatus == AIAvailabilityStatus.AVAILABLE

    suspend fun checkAvailability(): AIAvailabilityStatus {
        val status = client.checkStatus()
        updateAvailability(status)
        return status
    }

    fun updateAvailability(status: AIAvailabilityStatus) {
        metadata = metadata.copy(
            availabilityStatus = status,
            lastCheckedTimestamp = System.currentTimeMillis()
        )
    }

    suspend fun warmup() {
        if (isReady) {
            client.warmup()
        }
    }

    override suspend fun classifyIntent(input: String): SummerIntent = withContext(Dispatchers.Default) {
        if (!isReady) {
            return@withContext fallbackEngine.classifyIntent(input)
        }
        try {
            val prompt = "Classify intent: \"$input\"."
            val response = client.generateContent(prompt)
            if (response.isNullOrBlank()) {
                fallbackEngine.classifyIntent(input)
            } else {
                responseParser.parse(response, SummerInteraction(sessionId = "classifier", userInput = input)).intent
            }
        } catch (t: Throwable) {
            logWarn("classifyIntent error; using fallback: ${t.message}")
            fallbackEngine.classifyIntent(input)
        }
    }

    override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult = withContext(Dispatchers.Default) {
        if (!isReady) {
            logInfo("Gemini Nano not AVAILABLE (${metadata.availabilityStatus.name}); delegating to offline fallback.")
            return@withContext fallbackEngine.process(context, interaction)
        }

        try {
            val prompt = promptBuilder.buildPrompt(context, interaction)
            logInfo("Sending prompt to Gemini Nano (len=${prompt.length})")
            val generatedText = client.generateContent(prompt)

            if (!generatedText.isNullOrBlank()) {
                logInfo("Received Gemini Nano generation (len=${generatedText.length})")
                val parsed = responseParser.parse(
                    generatedText = generatedText,
                    interaction = interaction,
                    modelName = metadata.displayName
                )
                return@withContext parsed
            } else {
                logWarn("Empty response from Gemini Nano; using fallback engine.")
                return@withContext fallbackEngine.process(context, interaction)
            }
        } catch (t: Throwable) {
            logWarn("Exception during Gemini Nano inference; delegating to fallback: ${t.message}")
            return@withContext fallbackEngine.process(context, interaction)
        }
    }

    override suspend fun evaluateIntent(input: String): RecognizedIntent {
        val intent = classifyIntent(input)
        return when (intent) {
            is SummerIntent.Greeting -> RecognizedIntent.PersonalityQuery(input)
            is SummerIntent.IdentityQuestion -> RecognizedIntent.PersonalityQuery(input)
            is SummerIntent.CapabilityQuestion -> RecognizedIntent.SystemStatus(input)
            is SummerIntent.TimeQuery -> RecognizedIntent.SystemStatus(input)
            is SummerIntent.CapabilityAction -> RecognizedIntent.CapabilityRequest(intent.actionRequest)
            is SummerIntent.GeneralConversation -> RecognizedIntent.GeneralConversation(input)
            is SummerIntent.Unknown -> RecognizedIntent.GeneralConversation(input)
        }
    }

    override suspend fun processQuery(request: AIRequest): AIResponse = withContext(Dispatchers.Default) {
        if (!isReady) {
            return@withContext fallbackEngine.processQuery(request)
        }
        try {
            val text = client.generateContent(request.query)
            if (!text.isNullOrBlank()) {
                AIResponse(
                    text = text.trim(),
                    modelUsed = metadata.displayName,
                    recognizedIntent = evaluateIntent(request.query)
                )
            } else {
                fallbackEngine.processQuery(request)
            }
        } catch (_: Throwable) {
            fallbackEngine.processQuery(request)
        }
    }

    private fun logInfo(msg: String) {
        try {
            android.util.Log.i(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
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
