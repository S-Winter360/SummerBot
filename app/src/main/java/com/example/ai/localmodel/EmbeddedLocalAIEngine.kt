package com.example.ai.localmodel

import android.content.Context
import com.example.ai.AIEngine
import com.example.ai.OfflineLocalAIEngine
import com.example.ai.benchmark.ColdStartMetrics
import com.example.ai.benchmark.InferencePerformanceMetrics
import com.example.ai.capability.AIModelMetadata
import com.example.ai.capability.AIProviderType
import com.example.ai.capability.AIAvailabilityStatus
import com.example.ai.capability.AICapability
import com.example.ai.capability.GeminiResponseParser
import com.example.ai.capability.SummerPromptBuilder
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.personality.SummerPersonality
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable

/**
 * Real embedded local generative AI engine powered by Google's LiteRT-LM Android stack
 * (com.google.ai.edge.litertlm:litertlm-android).
 *
 * Runs completely offline after model file installation.
 * Provides on-device conversational text generation for devices without Gemini Nano support.
 */
class EmbeddedLocalAIEngine(
    private val context: Context,
    private val modelManager: EmbeddedModelManager = DefaultEmbeddedModelManager(context),
    private val fallbackEngine: OfflineLocalAIEngine = OfflineLocalAIEngine(),
    private val personality: SummerPersonality = SummerPersonality.DEFAULT,
    private val promptBuilder: SummerPromptBuilder = SummerPromptBuilder(personality = personality),
    private val responseParser: GeminiResponseParser = GeminiResponseParser()
) : AIEngine, DirectInferenceEngine, Closeable {

    companion object {
        private const val TAG = "EmbeddedLocalAIEngine"
    }

    private val engineMutex = Mutex()
    private var litertEngine: Engine? = null
    private var activeConversation: Conversation? = null
    private var currentLoadedPath: String? = null
    private var recordedInitializationTimeMs: Long? = null
    private var recordedFirstInferenceTimeMs: Long? = null

    override val isModelInstalledAndReady: Boolean
        get() = modelManager.isModelReady()

    var metadata: AIModelMetadata = AIModelMetadata(
        provider = AIProviderType.EMBEDDED_LOCAL_MODEL,
        modelIdentifier = "gemma-3-1b-it-litertlm",
        displayName = "Gemma 3 1B IT INT4",
        modelVersion = "1.0-4bit",
        availabilityStatus = if (modelManager.isModelReady()) AIAvailabilityStatus.AVAILABLE else AIAvailabilityStatus.UNAVAILABLE,
        supportedCapabilities = setOf(
            AICapability.TEXT_GENERATION,
            AICapability.CHAT,
            AICapability.STRUCTURED_OUTPUT,
            AICapability.SUMMARIZATION
        ),
        offlineCapable = true,
        multimodalSupport = false,
        runtime = "LiteRT-LM",
        quantization = "INT4",
        mode = "Offline"
    )
        private set

    override val modelInfo: AIModelInfo
        get() = AIModelInfo(
            name = metadata.displayName,
            version = metadata.modelVersion ?: "1.0",
            isLocalOffline = true,
            description = "Embedded local generative language model powered by Google LiteRT-LM."
        )

    override val isReady: Boolean
        get() = modelManager.isModelReady()

    /**
     * Initializes or gets the active LiteRT-LM Engine and Conversation.
     * Offloaded safely from the main thread.
     */
    suspend fun ensureInitialized(): Boolean = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            val modelPath = modelManager.getInstalledModelPath() ?: return@withContext false

            if (litertEngine != null && currentLoadedPath == modelPath) {
                return@withContext true
            }

            // Close existing resources if path changed
            closeInternal()

            try {
                logInfo("Initializing LiteRT-LM Engine for model at: $modelPath")

                // Baseline CPU Backend initialization
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU()
                )

                val initStart = android.os.SystemClock.elapsedRealtime()
                val engine = Engine(config)
                engine.initialize()
                recordedInitializationTimeMs = android.os.SystemClock.elapsedRealtime()

                litertEngine = engine
                currentLoadedPath = modelPath

                metadata = metadata.copy(
                    availabilityStatus = AIAvailabilityStatus.AVAILABLE,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
                logInfo("LiteRT-LM Engine initialized successfully.")
                true
            } catch (t: Throwable) {
                logError("Failed to initialize LiteRT-LM Engine; fallback will be used", t)
                closeInternal()
                metadata = metadata.copy(
                    availabilityStatus = AIAvailabilityStatus.ERROR,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
                false
            }
        }
    }

    override suspend fun classifyIntent(input: String): SummerIntent = withContext(Dispatchers.Default) {
        if (!isReady) {
            return@withContext fallbackEngine.classifyIntent(input)
        }
        try {
            val initialized = ensureInitialized()
            if (!initialized) {
                return@withContext fallbackEngine.classifyIntent(input)
            }

            val prompt = "Classify intent concisely: \"$input\"."
            val responseText = executeInference(prompt)

            if (responseText.isNullOrBlank()) {
                fallbackEngine.classifyIntent(input)
            } else {
                responseParser.parse(
                    generatedText = responseText,
                    interaction = SummerInteraction(sessionId = "classifier", userInput = input)
                ).intent
            }
        } catch (t: Throwable) {
            logWarn("classifyIntent error in LiteRT-LM; using fallback: ${t.message}")
            fallbackEngine.classifyIntent(input)
        }
    }

    override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult = withContext(Dispatchers.Default) {
        if (!isReady) {
            logInfo("Embedded model not ready; delegating to offline deterministic fallback.")
            return@withContext fallbackEngine.process(context, interaction)
        }

        try {
            val initialized = ensureInitialized()
            if (!initialized) {
                logWarn("Failed to initialize LiteRT-LM engine; delegating to deterministic fallback.")
                return@withContext fallbackEngine.process(context, interaction)
            }

            val prompt = promptBuilder.buildPrompt(context, interaction)
            logInfo("Executing LiteRT-LM inference (promptLen=${prompt.length})")

            val generatedText = executeInference(prompt)

            if (!generatedText.isNullOrBlank()) {
                logInfo("Received LiteRT-LM generation (len=${generatedText.length})")
                val parsed = responseParser.parse(
                    generatedText = generatedText,
                    interaction = interaction,
                    modelName = metadata.displayName
                )
                return@withContext parsed
            } else {
                logWarn("Empty response from LiteRT-LM; delegating to deterministic fallback.")
                return@withContext fallbackEngine.process(context, interaction)
            }
        } catch (t: Throwable) {
            logWarn("Exception during LiteRT-LM inference; delegating to deterministic fallback: ${t.message}")
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
            val initialized = ensureInitialized()
            if (!initialized) {
                return@withContext fallbackEngine.processQuery(request)
            }

            val text = executeInference(request.query)
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

    /**
     * Executes prompt inference using the active conversation.
     * Keeps latency bounded turn-after-turn by evaluating against the structured prompt
     * on the resident LiteRT-LM engine.
     */
    private suspend fun executeInference(prompt: String): String? = withContext(Dispatchers.IO) {
        engineMutex.withLock {
            val engine = litertEngine ?: return@withContext null
            val sb = StringBuilder()

            val systemPrompt = promptBuilder.buildSystemInstruction()
            val convConfig = ConversationConfig(
                systemInstruction = Contents.of(systemPrompt)
            )
            var conversation: Conversation? = null

            try {
                conversation = engine.createConversation(convConfig)
                val flow = conversation.sendMessageAsync(prompt)
                flow.collect { message ->
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val contents = message.contents.contents
                    for (c in contents) {
                        if (c is Content.Text) {
                            sb.append(c.text)
                        }
                    }
                }
                sb.toString().trim().ifBlank { null }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                logError("Inference execution failed", t)
                null
            } finally {
                try {
                    conversation?.close()
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Exposes streaming token flow for UI progressive rendering when requested.
     */
    fun streamInference(prompt: String): Flow<String> = flow {
        val initialized = ensureInitialized()
        if (!initialized) {
            emit(fallbackEngine.processQuery(AIRequest(prompt)).text)
            return@flow
        }

        engineMutex.withLock {
            val engine = litertEngine ?: return@flow
            val systemPrompt = promptBuilder.buildSystemInstruction()
            val convConfig = ConversationConfig(
                systemInstruction = Contents.of(systemPrompt)
            )
            var conversation: Conversation? = null
            try {
                conversation = engine.createConversation(convConfig)
                val flow = conversation.sendMessageAsync(prompt)
                flow.collect { message ->
                    val contents = message.contents.contents
                    for (c in contents) {
                        if (c is Content.Text) {
                            emit(c.text)
                        }
                    }
                }
            } finally {
                try {
                    conversation?.close()
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Executes prompt inference directly on LiteRT-LM for developer benchmarking.
     * Captures precise monotonic latency and reads conversation.getBenchmarkInfo() token metrics.
     * Bypasses all orchestrators, decision engines, and deterministic fallbacks.
     */
    @OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)
    override suspend fun executeDirectInference(
        prompt: String,
        systemInstruction: String?
    ): DirectInferenceResult = withContext(Dispatchers.IO) {
        if (!isModelInstalledAndReady) {
            throw IllegalStateException("Embedded model is not installed or ready.")
        }
        val initialized = ensureInitialized()
        if (!initialized) {
            throw IllegalStateException("Failed to initialize LiteRT-LM engine.")
        }

        engineMutex.withLock {
            val engine = litertEngine ?: throw IllegalStateException("LiteRT-LM engine is null despite initialization.")
            val sb = StringBuilder()

            val convConfig = if (systemInstruction.isNullOrBlank()) {
                ConversationConfig()
            } else {
                ConversationConfig(systemInstruction = Contents.of(systemInstruction))
            }

            var conversation: Conversation? = null
            val startTime = android.os.SystemClock.elapsedRealtime()
            var timeToFirstTokenMs: Long? = null

            try {
                conversation = engine.createConversation(convConfig)
                val flow = conversation.sendMessageAsync(prompt)
                flow.collect { message ->
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val contents = message.contents.contents
                    for (c in contents) {
                        if (c is Content.Text) {
                            if (timeToFirstTokenMs == null && c.text.isNotEmpty()) {
                                timeToFirstTokenMs = android.os.SystemClock.elapsedRealtime() - startTime
                            }
                            sb.append(c.text)
                        }
                    }
                }
                val totalInferenceTimeMs = android.os.SystemClock.elapsedRealtime() - startTime
                val ttftMs = timeToFirstTokenMs ?: totalInferenceTimeMs

                if (recordedFirstInferenceTimeMs == null) {
                    recordedFirstInferenceTimeMs = totalInferenceTimeMs
                }

                var benchInfo: com.google.ai.edge.litertlm.BenchmarkInfo? = null
                try {
                    benchInfo = conversation.getBenchmarkInfo()
                } catch (t: Throwable) {
                    logWarn("BenchmarkInfo not accessible from conversation: ${t.message}")
                }

                val inputTokens = benchInfo?.lastPrefillTokenCount?.takeIf { it > 0 }
                val outputTokens = benchInfo?.lastDecodeTokenCount?.takeIf { it > 0 }
                val tps = benchInfo?.lastDecodeTokensPerSecond?.takeIf { it > 0.0 }
                val prefillTps = benchInfo?.lastPrefillTokensPerSecond?.takeIf { it > 0.0 }
                val initTimeSec = benchInfo?.initTimeInSecond?.takeIf { it > 0.0 }

                DirectInferenceResult(
                    outputText = sb.toString().trim(),
                    timeToFirstResponseMs = ttftMs,
                    totalInferenceTimeMs = totalInferenceTimeMs,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    tokensPerSecond = tps,
                    prefillTokensPerSecond = prefillTps,
                    initTimeInSecond = initTimeSec,
                    rawBenchmarkInfoAvailable = benchInfo != null && (inputTokens != null || outputTokens != null || tps != null)
                )
            } finally {
                try {
                    conversation?.close()
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Measures cold start metrics (engine initialization + first inference latency).
     */
    override suspend fun measureColdStart(samplePrompt: String): ColdStartMetrics = withContext(Dispatchers.IO) {
        if (!isModelInstalledAndReady) {
            throw IllegalStateException("Embedded model is not installed or ready.")
        }

        // If engine was already resident and we have recorded initialization metrics from this session:
        if (litertEngine != null && recordedInitializationTimeMs != null && recordedFirstInferenceTimeMs != null) {
            return@withContext ColdStartMetrics(
                modelInitializationTimeMs = recordedInitializationTimeMs!!,
                firstInferenceTimeMs = recordedFirstInferenceTimeMs!!,
                totalColdStartTimeMs = recordedInitializationTimeMs!! + recordedFirstInferenceTimeMs!!,
                wasAlreadyInitialized = true
            )
        }

        engineMutex.withLock {
            closeInternal()
            val modelPath = modelManager.getInstalledModelPath()
                ?: throw IllegalStateException("Model path not found.")

            val initStart = android.os.SystemClock.elapsedRealtime()
            val config = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU()
            )
            val engine = Engine(config)
            engine.initialize()
            litertEngine = engine
            currentLoadedPath = modelPath
            val initDuration = android.os.SystemClock.elapsedRealtime() - initStart
            recordedInitializationTimeMs = initDuration

            val inferStart = android.os.SystemClock.elapsedRealtime()
            var conversation: Conversation? = null
            try {
                val convConfig = ConversationConfig()
                conversation = engine.createConversation(convConfig)
                val flow = conversation.sendMessageAsync(samplePrompt)
                flow.collect { }
            } finally {
                try {
                    conversation?.close()
                } catch (_: Throwable) {}
            }
            val firstInferDuration = android.os.SystemClock.elapsedRealtime() - inferStart
            recordedFirstInferenceTimeMs = firstInferDuration

            ColdStartMetrics(
                modelInitializationTimeMs = initDuration,
                firstInferenceTimeMs = firstInferDuration,
                totalColdStartTimeMs = initDuration + firstInferDuration,
                wasAlreadyInitialized = false
            )
        }
    }

    override fun getRuntimeModelDiagnostics(): Map<String, String> {
        return mapOf(
            "Model" to metadata.displayName,
            "Runtime" to (metadata.runtime ?: "LiteRT-LM"),
            "Quantization" to (metadata.quantization ?: "INT4"),
            "Backend" to "CPU (Configured)",
            "Engine Loaded" to (litertEngine != null).toString(),
            "Model Path" to (currentLoadedPath ?: modelManager.getInstalledModelPath() ?: "Not installed")
        )
    }

    private fun closeInternal() {
        try {
            activeConversation?.close()
        } catch (_: Throwable) {}
        activeConversation = null

        try {
            litertEngine?.close()
        } catch (_: Throwable) {}
        litertEngine = null
        currentLoadedPath = null
    }

    override fun close() {
        closeInternal()
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

    private fun logError(msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(TAG, msg, tr)
        } catch (_: Throwable) {
            System.err.println("[$TAG] $msg: ${tr?.message}")
        }
    }
}
