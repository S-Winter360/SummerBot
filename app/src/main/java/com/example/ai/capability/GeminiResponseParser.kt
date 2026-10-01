package com.example.ai.capability

import com.example.actions.ActionRequest
import com.example.ai.models.AIResult
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryRecord
import com.example.security.Capability
import java.util.Locale

/**
 * Safely parses on-device Gemini Nano generated text into Summer's structured cognitive contracts:
 * [AIResult], [SummerIntent], [SummerResponse], [ActionRequest], and [MemoryRecord].
 *
 * Guarantees that:
 * 1. Malformed model text never causes exceptions.
 * 2. Actions proposed by the model are isolated inside [ActionRequest] for downstream security policy evaluation.
 * 3. Memory storage only occurs when the user explicitly requests to remember something.
 */
class GeminiResponseParser {

    fun parse(
        generatedText: String,
        interaction: SummerInteraction,
        modelName: String = "Gemini Nano (ML Kit Prompt API)"
    ): AIResult {
        var cleanText = generatedText.trim()
        if (cleanText.startsWith("Summer:", ignoreCase = true)) {
            cleanText = cleanText.substringAfter("Summer:").trim()
        }
        if (cleanText.isBlank()) {
            cleanText = "I processed your request, but received an empty response."
        }

        val userInput = interaction.userInput.trim()
        val normalizedInput = userInput.lowercase(Locale.ROOT)

        val intent = classifyIntent(userInput, normalizedInput, cleanText)
        val actionRequests = extractActionProposals(normalizedInput)
        val memorySuggestions = extractMemoryProposals(userInput, normalizedInput)

        val responseType = when {
            actionRequests.isNotEmpty() -> ResponseType.ACTION_PROPOSAL
            cleanText.endsWith("?") -> ResponseType.QUESTION
            intent is SummerIntent.IdentityQuestion || intent is SummerIntent.CapabilityQuestion -> ResponseType.INFORMATION
            else -> ResponseType.TEXT
        }

        val response = SummerResponse(
            text = cleanText,
            type = responseType,
            suggestedActions = actionRequests,
            confidence = 0.95f,
            source = modelName,
            timestamp = System.currentTimeMillis()
        )

        return AIResult(
            intent = intent,
            response = response,
            actionRequests = actionRequests,
            memorySuggestions = memorySuggestions,
            confidence = 0.95f
        )
    }

    private fun classifyIntent(
        rawInput: String,
        normalizedInput: String,
        generatedResponse: String
    ): SummerIntent {
        return when {
            normalizedInput.startsWith("hello") || normalizedInput.startsWith("hi") ||
                normalizedInput.startsWith("hey") || normalizedInput.contains("good morning") ||
                normalizedInput.contains("greetings") ->
                SummerIntent.Greeting(rawInput)

            normalizedInput.contains("your name") || normalizedInput.contains("who are you") ||
                normalizedInput.contains("what are you") || normalizedInput.contains("tell me about yourself") ->
                SummerIntent.IdentityQuestion(rawInput)

            normalizedInput.contains("what can you do") || normalizedInput.contains("capabilities") ||
                normalizedInput.contains("help me with") || normalizedInput == "help" ->
                SummerIntent.CapabilityQuestion(rawInput)

            normalizedInput.contains("what time is it") || normalizedInput.contains("current time") ||
                normalizedInput.contains("what is the time") || normalizedInput == "time" ->
                SummerIntent.TimeQuery(rawInput)

            normalizedInput.contains("test network") || normalizedInput.contains("connect internet") ->
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.INTERNET,
                        actionName = "Check External Connectivity",
                        reasoning = "User requested network verification through intent."
                    ),
                    rawQuery = rawInput
                )

            normalizedInput.contains("test mic") || normalizedInput.contains("test audio") ||
                normalizedInput.contains("test microphone") ->
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.MICROPHONE,
                        actionName = "Activate Audio Stream",
                        reasoning = "User requested audio test through intent."
                    ),
                    rawQuery = rawInput
                )

            normalizedInput.contains("test camera") || normalizedInput.contains("test vision") ->
                SummerIntent.CapabilityAction(
                    ActionRequest(
                        capability = Capability.CAMERA,
                        actionName = "Analyze Visual Frame",
                        reasoning = "User requested vision test through intent."
                    ),
                    rawQuery = rawInput
                )

            else -> SummerIntent.GeneralConversation(generatedResponse, rawQuery = rawInput)
        }
    }

    private fun extractActionProposals(normalizedInput: String): List<ActionRequest> {
        val requests = mutableListOf<ActionRequest>()
        if (normalizedInput.contains("test network") || normalizedInput.contains("check network")) {
            requests.add(
                ActionRequest(
                    capability = Capability.INTERNET,
                    actionName = "Check External Connectivity",
                    reasoning = "Verified internet access request."
                )
            )
        }
        if (normalizedInput.contains("test mic") || normalizedInput.contains("test microphone")) {
            requests.add(
                ActionRequest(
                    capability = Capability.MICROPHONE,
                    actionName = "Activate Audio Stream",
                    reasoning = "Microphone diagnostic check."
                )
            )
        }
        if (normalizedInput.contains("test camera") || normalizedInput.contains("test vision")) {
            requests.add(
                ActionRequest(
                    capability = Capability.CAMERA,
                    actionName = "Analyze Visual Frame",
                    reasoning = "Camera frame diagnostic check."
                )
            )
        }
        return requests
    }

    private fun extractMemoryProposals(rawInput: String, normalizedInput: String): List<MemoryRecord> {
        val memories = mutableListOf<MemoryRecord>()
        if (normalizedInput.startsWith("remember ") || normalizedInput.startsWith("remember:")) {
            var fact = rawInput.substringAfter("remember ", "").trim()
            if (fact.isBlank()) {
                fact = rawInput.substringAfter("Remember ", "").trim()
            }
            if (fact.startsWith("that ", ignoreCase = true)) {
                fact = fact.substring(5).trim()
            }
            if (fact.isNotBlank()) {
                memories.add(
                    MemoryRecord(
                        category = MemoryCategory.USER_PREFERENCE,
                        title = "Explicit Memory",
                        content = fact,
                        confidence = 1.0f
                    )
                )
            }
        }
        return memories
    }
}
