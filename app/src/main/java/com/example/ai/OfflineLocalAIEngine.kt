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
import com.example.core.response.MemoryOperation
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryImportance
import com.example.memory.models.MemoryRecord
import com.example.memory.models.MemorySource
import com.example.security.Capability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            normalized.isEmpty() -> SummerIntent.Unknown(input)

            normalized.startsWith("hello") || normalized.startsWith("hi") ||
                normalized.startsWith("hey") || normalized.contains("greetings") ||
                normalized.contains("good morning") ->
                SummerIntent.Greeting(input)

            normalized.contains("your name") || normalized.contains("who are you") ||
                normalized.contains("what are you") || normalized.contains("tell me about yourself") ->
                SummerIntent.IdentityQuestion(input)

            normalized.contains("what can you do") || normalized.contains("capabilities") ||
                normalized.contains("help me with") || normalized == "help" ->
                SummerIntent.CapabilityQuestion(input)

            normalized.contains("what time is it") || normalized.contains("current time") ||
                normalized.contains("what is the time") || normalized == "time" ->
                SummerIntent.TimeQuery(input)

            normalized.contains("test network") || normalized.contains("connect internet") ->
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.INTERNET,
                        actionName = "Check External Connectivity",
                        reasoning = "User requested verification of external network access via capability gate."
                    ),
                    rawQuery = input
                )

            normalized.contains("test mic") || normalized.contains("test audio") || normalized.contains("listen") ->
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.MICROPHONE,
                        actionName = "Activate Audio Stream",
                        reasoning = "User initiated request to test microphone capture authorization."
                    ),
                    rawQuery = input
                )

            normalized.contains("test camera") || normalized.contains("test vision") ->
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.CAMERA,
                        actionName = "Analyze Visual Frame",
                        reasoning = "User initiated request to inspect camera frame authorization."
                    ),
                    rawQuery = input
                )

            normalized.startsWith("remember ") || normalized.startsWith("keep in mind ") ||
                normalized.startsWith("please remember ") || normalized.startsWith("save this: ") -> {
                val fact = input.substringAfter("remember ", input).trim()
                SummerIntent.GeneralConversation("Remembering: $fact", rawQuery = input)
            }

            normalized.startsWith("forget ") || normalized.startsWith("delete memory ") -> {
                val target = input.substringAfter("forget ", input).trim()
                SummerIntent.GeneralConversation("Forgetting: $target", rawQuery = input)
            }

            normalized.startsWith("actually, ") || normalized.startsWith("update my ") ||
                normalized.startsWith("change my ") -> {
                SummerIntent.GeneralConversation("Updating memory: $input", rawQuery = input)
            }

            else -> SummerIntent.GeneralConversation(input, rawQuery = input)
        }
    }

    override suspend fun process(context: SummerContext, interaction: SummerInteraction): AIResult {
        val userInput = interaction.userInput.trim()
        val normalized = userInput.lowercase(Locale.ROOT)
        val intent = classifyIntent(userInput)

        return when (intent) {
            is SummerIntent.Greeting -> {
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = "Hello. I'm ${personality.shortName}. How can I assist you?",
                        type = ResponseType.TEXT,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.IdentityQuestion -> {
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = "I'm ${personality.fullName}. You can call me ${personality.shortName}.",
                        type = ResponseType.INFORMATION,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.CapabilityQuestion -> {
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = "I am an offline-first cognitive companion. My current local architecture is being prepared for on-device generative reasoning, action security gating, and local memory.",
                        type = ResponseType.INFORMATION,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.TimeQuery -> {
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = "The current local time is $timeStr.",
                        type = ResponseType.INFORMATION,
                        source = modelInfo.name
                    ),
                    confidence = 1.0f
                )
            }

            is SummerIntent.CapabilityAction -> {
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = "Initiating authorized execution for [${intent.actionRequest.actionName}].",
                        type = ResponseType.ACTION_PROPOSAL,
                        suggestedActions = listOf(intent.actionRequest),
                        source = modelInfo.name
                    ),
                    actionRequests = listOf(intent.actionRequest),
                    confidence = 1.0f
                )
            }

            is SummerIntent.GeneralConversation -> {
                // Check for explicit memory commands
                if (normalized.startsWith("remember ") || normalized.startsWith("keep in mind ") ||
                    normalized.startsWith("please remember ") || normalized.startsWith("save this: ")
                ) {
                    var fact = when {
                        normalized.startsWith("remember ") -> userInput.substringAfter("remember ").trim()
                        normalized.startsWith("keep in mind ") -> userInput.substringAfter("keep in mind ").trim()
                        normalized.startsWith("please remember ") -> userInput.substringAfter("please remember ").trim()
                        normalized.startsWith("save this: ") -> userInput.substringAfter("save this: ").trim()
                        else -> userInput
                    }
                    if (fact.startsWith("that ", ignoreCase = true)) {
                        fact = fact.substring(5).trim()
                    }

                    val memoryRecord = MemoryRecord(
                        category = MemoryCategory.PREFERENCE,
                        title = "User Preference",
                        content = fact,
                        source = MemorySource.EXPLICIT_USER,
                        importance = MemoryImportance.NORMAL,
                        confidence = 1.0f
                    )
                    AIResult(
                        intent = intent,
                        response = SummerResponse(
                            text = "I have noted that in local memory: \"$fact\".",
                            type = ResponseType.TEXT,
                            memoryOperations = listOf(MemoryOperation.Store(memoryRecord)),
                            source = modelInfo.name
                        ),
                        memorySuggestions = listOf(memoryRecord),
                        confidence = 1.0f
                    )
                } else if (normalized.startsWith("forget ") || normalized.startsWith("delete memory ")) {
                    val target = when {
                        normalized.startsWith("forget that ") -> userInput.substringAfter("forget that ").trim()
                        normalized.startsWith("delete memory ") -> userInput.substringAfter("delete memory ").trim()
                        else -> userInput.substringAfter("forget ").trim()
                    }
                    val op = MemoryOperation.Forget(
                        keywordOrContent = target,
                        reason = "User requested forget command"
                    )
                    AIResult(
                        intent = intent,
                        response = SummerResponse(
                            text = "I have processed your request to forget: \"$target\".",
                            type = ResponseType.TEXT,
                            memoryOperations = listOf(op),
                            source = modelInfo.name
                        ),
                        confidence = 1.0f
                    )
                } else if (normalized.startsWith("actually, ") || normalized.startsWith("update my ")) {
                    val updatedFact = when {
                        normalized.startsWith("actually, ") -> userInput.substringAfter("actually, ").trim()
                        normalized.startsWith("update my ") -> userInput.substringAfter("update my ").trim()
                        else -> userInput
                    }
                    val updatedRecord = MemoryRecord(
                        category = MemoryCategory.PREFERENCE,
                        title = "Updated Preference",
                        content = updatedFact,
                        source = MemorySource.USER_UPDATE,
                        importance = MemoryImportance.NORMAL,
                        confidence = 1.0f
                    )
                    val op = MemoryOperation.Update(
                        record = updatedRecord,
                        previousContent = updatedFact
                    )
                    AIResult(
                        intent = intent,
                        response = SummerResponse(
                            text = "I have updated your memory to: \"$updatedFact\".",
                            type = ResponseType.TEXT,
                            memoryOperations = listOf(op),
                            source = modelInfo.name
                        ),
                        memorySuggestions = listOf(updatedRecord),
                        confidence = 1.0f
                    )
                } else {
                    // Answer from retrieved relevant memories if available in context
                    val relevantMemories = context.memoryContext?.relevantMemories ?: context.activeMemories
                    if (relevantMemories.isNotEmpty()) {
                        val memorySnippet = relevantMemories.first().content
                        AIResult(
                            intent = intent,
                            response = SummerResponse(
                                text = "Based on your memory: $memorySnippet",
                                type = ResponseType.TEXT,
                                source = modelInfo.name
                            ),
                            confidence = 0.9f
                        )
                    } else {
                        AIResult(
                            intent = intent,
                            response = SummerResponse(
                                text = "Received: ${intent.text}. Deterministic engine has logged this interaction.",
                                type = ResponseType.TEXT,
                                source = modelInfo.name
                            ),
                            confidence = 0.8f
                        )
                    }
                }
            }

            is SummerIntent.Unknown -> {
                AIResult(
                    intent = intent,
                    response = SummerResponse(
                        text = "I don't have enough understanding for that yet. My local intelligence module is still being developed.",
                        type = ResponseType.TEXT,
                        source = modelInfo.name
                    ),
                    confidence = 0.0f
                )
            }
        }
    }

    override suspend fun processQuery(request: AIRequest): AIResponse {
        val intent = classifyIntent(request.query)
        val recognized = when (intent) {
            is SummerIntent.Greeting -> RecognizedIntent.GeneralConversation("Greeting")
            is SummerIntent.IdentityQuestion -> RecognizedIntent.PersonalityQuery(request.query)
            is SummerIntent.CapabilityQuestion -> RecognizedIntent.SystemStatus("Capabilities")
            is SummerIntent.TimeQuery -> RecognizedIntent.SystemStatus("Time")
            is SummerIntent.CapabilityAction -> RecognizedIntent.CapabilityRequest(intent.actionRequest)
            else -> RecognizedIntent.GeneralConversation(request.query)
        }
        return AIResponse(
            text = "Legacy query processed by ${modelInfo.name}.",
            modelUsed = modelInfo.name,
            recognizedIntent = recognized
        )
    }

    override suspend fun evaluateIntent(input: String): RecognizedIntent {
        return RecognizedIntent.GeneralConversation(input)
    }
}
