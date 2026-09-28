package com.example.ai

import com.example.actions.ActionRequest
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.AIResult
import com.example.ai.models.RecognizedIntent
import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.personality.SummerPersonality
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.security.Capability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Deterministic local reasoning engine for Summer's architectural foundation phase.
 * Demonstrates the intent classification, context ingestion, and structured result generation
 * without pretending to run a heavyweight LLM.
 */
class OfflineLocalAIEngine(
    private val personality: SummerPersonality = SummerPersonality.DEFAULT
) : AIEngine {

    override val modelInfo: AIModelInfo = AIModelInfo(
        name = "Summer Offline Core",
        version = "0.2.0-alpha",
        isLocalOffline = true,
        description = "Deterministic cognitive intent parser and architecture demonstrator."
    )

    override val isReady: Boolean = true

    override suspend fun classifyIntent(input: String): SummerIntent {
        val normalized = input.trim().lowercase(Locale.ROOT)

        return when {
            normalized.isEmpty() -> {
                SummerIntent.Unknown(input)
            }

            // Greeting matches: "hello", "hello summer", "hi", "hey summer", "greetings"
            normalized.startsWith("hello") || normalized.startsWith("hi") ||
                normalized.startsWith("hey") || normalized.contains("greetings") -> {
                SummerIntent.Greeting(input)
            }

            // Identity matches: "what is your name", "who are you", "what's your name"
            normalized.contains("your name") || normalized.contains("who are you") -> {
                SummerIntent.IdentityQuestion(input)
            }

            // Capability matches: "what can you do", "help", "capabilities"
            normalized.contains("what can you do") || normalized.contains("capabilities") ||
                normalized == "help" -> {
                SummerIntent.CapabilityQuestion(input)
            }

            // Time query matches: "what time is it", "current time", "what is the time", "time"
            normalized.contains("what time is it") || normalized.contains("current time") ||
                normalized.contains("what is the time") || normalized == "time" -> {
                SummerIntent.TimeQuery(input)
            }

            // Capability action: Network test
            normalized.contains("test network") || normalized.contains("connect internet") -> {
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.INTERNET,
                        actionName = "Check External Connectivity",
                        reasoning = "User requested verification of external network access via capability gate."
                    ),
                    rawQuery = input
                )
            }

            // Capability action: Microphone test
            normalized.contains("test mic") || normalized.contains("test audio") || normalized.contains("listen") -> {
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.MICROPHONE,
                        actionName = "Activate Audio Stream",
                        reasoning = "User initiated request to test microphone capture authorization."
                    ),
                    rawQuery = input
                )
            }

            // Capability action: Camera test
            normalized.contains("test camera") || normalized.contains("test vision") || normalized.contains("inspect camera") -> {
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.CAMERA,
                        actionName = "Analyze Visual Frame",
                        reasoning = "User initiated request to test vision sensor authorization."
                    ),
                    rawQuery = input
                )
            }

            // Capability action: Memory persistence
            normalized.startsWith("remember") || normalized.startsWith("save memory") || normalized.startsWith("note:") -> {
                val contentToSave = input.removePrefix("remember").removePrefix("save memory").removePrefix("note:").trim()
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.LOCAL_MEMORY_WRITE,
                        actionName = "Persist Memory Record",
                        parameters = mapOf("content" to contentToSave.ifEmpty { input }),
                        reasoning = "User explicitly requested storing data into local memory repository."
                    ),
                    rawQuery = input
                )
            }

            else -> {
                SummerIntent.Unknown(input)
            }
        }
    }

    override suspend fun process(
        context: SummerContext,
        interaction: SummerInteraction
    ): AIResult {
        val intent = classifyIntent(interaction.userInput)

        return when (intent) {
            is SummerIntent.Greeting -> {
                val text = "Hello. I'm ${personality.shortName}. How can I assist you?"
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = text,
                        type = ResponseType.TEXT,
                        confidence = 1.0f,
                        isSpeechAppropriate = true,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.IdentityQuestion -> {
                val text = "I'm ${personality.name}. You can call me ${personality.shortName}."
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = text,
                        type = ResponseType.INFORMATION,
                        confidence = 1.0f,
                        isSpeechAppropriate = true,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.CapabilityQuestion -> {
                val text = "I am currently under active development. My architecture is being prepared for future voice, persistent memory, vision, device-control, and on-device AI capabilities."
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = text,
                        type = ResponseType.INFORMATION,
                        confidence = 1.0f,
                        isSpeechAppropriate = true,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.TimeQuery -> {
                val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                val text = "It is currently $currentTime."
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = text,
                        type = ResponseType.INFORMATION,
                        confidence = 1.0f,
                        isSpeechAppropriate = true,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.CapabilityAction -> {
                val action = intent.actionRequest
                val proposalText = "Action requested: [${action.actionName}]. Routing through Summer's security authorization gatekeeper."
                val memorySuggestions = if (action.capability == Capability.LOCAL_MEMORY_WRITE) {
                    val content = action.parameters["content"] ?: interaction.userInput
                    listOf(
                        MemoryRecord(
                            category = MemoryCategory.LEARNED_FACT,
                            title = "Explicit User Fact",
                            content = content
                        )
                    )
                } else {
                    emptyList()
                }

                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = proposalText,
                        type = ResponseType.ACTION_PROPOSAL,
                        confidence = 1.0f,
                        actionRequests = listOf(action),
                        source = modelInfo.name
                    ),
                    actionRequests = listOf(action),
                    memorySuggestions = memorySuggestions,
                    confidence = 1.0f
                )
            }

            is SummerIntent.GeneralConversation -> {
                val text = "I received your query. In this architectural phase, local model pipelines and cognitive state transitions are verified."
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = text,
                        type = ResponseType.TEXT,
                        confidence = intent.confidence,
                        source = modelInfo.name
                    ),
                    confidence = intent.confidence
                )
            }

            is SummerIntent.Unknown -> {
                val text = "I don't have enough understanding for that yet. My local intelligence module is still being developed."
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = text,
                        type = ResponseType.TEXT,
                        confidence = 0.2f,
                        source = modelInfo.name
                    ),
                    confidence = 0.2f
                )
            }
        }
    }

    // --- Backward compatibility implementations ---

    override suspend fun evaluateIntent(input: String): RecognizedIntent {
        val classified = classifyIntent(input)
        return when (classified) {
            is SummerIntent.Greeting -> RecognizedIntent.GeneralConversation(input)
            is SummerIntent.IdentityQuestion -> RecognizedIntent.PersonalityQuery(input)
            is SummerIntent.CapabilityQuestion -> RecognizedIntent.SystemStatus(input)
            is SummerIntent.TimeQuery -> RecognizedIntent.GeneralConversation(input)
            is SummerIntent.CapabilityAction -> RecognizedIntent.CapabilityRequest(classified.actionRequest)
            is SummerIntent.GeneralConversation -> RecognizedIntent.GeneralConversation(input)
            is SummerIntent.Unknown -> RecognizedIntent.GeneralConversation(input)
        }
    }

    override suspend fun processQuery(request: AIRequest): AIResponse {
        val dummyInteraction = SummerInteraction(
            sessionId = request.conversationId,
            userInput = request.query
        )
        val dummyContext = SummerContext(sessionId = request.conversationId)
        val result = process(dummyContext, dummyInteraction)
        val legacyIntent = evaluateIntent(request.query)

        return AIResponse(
            text = result.response.text,
            modelUsed = modelInfo.name,
            recognizedIntent = legacyIntent,
            requiredAction = result.actionRequests.firstOrNull()
        )
    }
}
