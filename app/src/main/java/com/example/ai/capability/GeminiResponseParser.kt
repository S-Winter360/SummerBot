package com.example.ai.capability

import com.example.actions.ActionRequest
import com.example.ai.models.AIResult
import com.example.core.interaction.SummerInteraction
import com.example.core.intent.SummerIntent
import com.example.core.response.MemoryOperation
import com.example.core.response.ResponseType
import com.example.core.response.SummerResponse
import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryImportance
import com.example.memory.models.MemoryRecord
import com.example.memory.models.MemorySource
import com.example.security.Capability
import java.util.Locale

/**
 * Safely parses on-device Gemini Nano generated text into Summer's structured cognitive contracts:
 * [AIResult], [SummerIntent], [SummerResponse], [ActionRequest], and [MemoryRecord] / [MemoryOperation].
 *
 * Guarantees that:
 * 1. Malformed model text never causes exceptions.
 * 2. Actions proposed by the model are isolated inside [ActionRequest] for downstream security policy evaluation.
 * 3. Memory storage, updates, and forgetting only occur when the user explicitly requests them.
 * 4. Normal conversational statements NEVER create permanent memory proposals.
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
        val memoryOperations = extractMemoryOperations(userInput, normalizedInput)
        val memorySuggestions = memoryOperations.mapNotNull {
            when (it) {
                is MemoryOperation.Store -> it.record
                is MemoryOperation.Update -> it.record
                else -> null
            }
        }

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
            memoryOperations = memoryOperations,
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

    fun extractMemoryOperations(rawInput: String, normalizedInput: String): List<MemoryOperation> {
        val operations = mutableListOf<MemoryOperation>()

        // 1. Explicit FORGET commands
        if (normalizedInput.startsWith("forget ") || normalizedInput.startsWith("delete memory ") ||
            normalizedInput.startsWith("forget that ") || normalizedInput.startsWith("forget what i told you about ")
        ) {
            val target = when {
                normalizedInput.startsWith("forget what i told you about ") ->
                    rawInput.substringAfter("forget what i told you about ", rawInput).trim()
                normalizedInput.startsWith("forget that ") -> {
                    val rawMatch = rawInput.substringAfter("forget that ", "").ifBlank {
                        rawInput.substringAfter("Forget that ", "")
                    }.trim()
                    rawMatch.ifBlank { rawInput.substringAfter("forget that ", rawInput).trim() }
                }
                normalizedInput.startsWith("delete memory ") ->
                    rawInput.substringAfter("delete memory ", rawInput).trim()
                else -> {
                    val rawMatch = rawInput.substringAfter("forget ", "").ifBlank {
                        rawInput.substringAfter("Forget ", "")
                    }.trim()
                    rawMatch.ifBlank { rawInput.substringAfter("forget ", rawInput).trim() }
                }
            }
            if (target.isNotBlank()) {
                operations.add(
                    MemoryOperation.Forget(
                        keywordOrContent = target,
                        reason = "Explicit user forget instruction"
                    )
                )
            }
            return operations
        }

        // 2. Explicit UPDATE commands & natural user corrections
        if (normalizedInput.startsWith("actually, ") || normalizedInput.startsWith("update my ") ||
            normalizedInput.startsWith("change my ")
        ) {
            var updatedFact = when {
                normalizedInput.startsWith("actually, ") -> {
                    val rawMatch = rawInput.substringAfter("actually, ", "").ifBlank {
                        rawInput.substringAfter("Actually, ", "")
                    }.trim()
                    rawMatch.ifBlank { rawInput.substringAfter("actually, ", rawInput).trim() }
                }
                normalizedInput.startsWith("update my ") ->
                    rawInput.substringAfter("update my ", rawInput).trim()
                normalizedInput.startsWith("change my ") ->
                    rawInput.substringAfter("change my ", rawInput).trim()
                else -> ""
            }
            if (updatedFact.isNotBlank()) {
                val record = MemoryRecord(
                    category = MemoryCategory.USER_PREFERENCE,
                    title = "Updated Preference",
                    content = updatedFact,
                    source = MemorySource.USER_UPDATE,
                    importance = MemoryImportance.NORMAL,
                    confidence = 1.0f
                )
                operations.add(
                    MemoryOperation.Update(
                        record = record,
                        previousContent = updatedFact
                    )
                )
            }
            return operations
        }

        // 2b. Natural user corrections (e.g. "My favourite language isn't Java. It's Python.")
        val hasNegation = normalizedInput.contains(" isn't ") || normalizedInput.contains(" is not ")
        val hasReplacement = normalizedInput.contains(" it's ") || normalizedInput.contains(" it is ") ||
            normalizedInput.contains(". it's ") || normalizedInput.contains(". it is ") ||
            normalizedInput.contains(".it's") || normalizedInput.contains(".it is")
        if (hasNegation && hasReplacement) {
            val separator = if (normalizedInput.contains(" isn't ")) " isn't " else " is not "
            val afterNot = rawInput.substringAfter(separator, "").trim()
            val previousTarget = when {
                afterNot.contains(". ") -> afterNot.substringBefore(". ")
                afterNot.contains(".") -> afterNot.substringBefore(".")
                afterNot.contains("It's", ignoreCase = true) -> afterNot.substring(0, afterNot.indexOf("It's", ignoreCase = true))
                afterNot.contains("It is", ignoreCase = true) -> afterNot.substring(0, afterNot.indexOf("It is", ignoreCase = true))
                else -> afterNot
            }.trim()

            val newPreference = when {
                afterNot.contains("It's") -> afterNot.substringAfter("It's")
                afterNot.contains("it's") -> afterNot.substringAfter("it's")
                afterNot.contains("It is") -> afterNot.substringAfter("It is")
                afterNot.contains("it is") -> afterNot.substringAfter("it is")
                else -> ""
            }.trim().trimStart(':', ' ').trimEnd('.')

            if (newPreference.isNotBlank()) {
                val subject = rawInput.substringBefore(separator).trim()
                val updatedContent = if (subject.isNotBlank()) "$subject: $newPreference" else newPreference
                val record = MemoryRecord(
                    category = determineCategory(updatedContent),
                    title = "Corrected Preference",
                    content = updatedContent,
                    source = MemorySource.USER_UPDATE,
                    importance = MemoryImportance.NORMAL,
                    confidence = 1.0f
                )
                operations.add(
                    MemoryOperation.Update(
                        record = record,
                        previousContent = previousTarget.ifBlank { null }
                    )
                )
                return operations
            }
        }

        // 3. Explicit STORE commands
        if (normalizedInput.startsWith("remember ") || normalizedInput.startsWith("remember:") ||
            normalizedInput.startsWith("keep in mind ") || normalizedInput.startsWith("please remember ") ||
            normalizedInput.startsWith("save this: ")
        ) {
            var fact = when {
                normalizedInput.startsWith("remember ") -> {
                    val rawMatch = rawInput.substringAfter("remember ", "").ifBlank {
                        rawInput.substringAfter("Remember ", "")
                    }.trim()
                    rawMatch.ifBlank { rawInput.substringAfter("remember ", rawInput).trim() }
                }
                normalizedInput.startsWith("remember:") -> rawInput.substringAfter("remember:", rawInput).trim()
                normalizedInput.startsWith("keep in mind ") -> rawInput.substringAfter("keep in mind ", rawInput).trim()
                normalizedInput.startsWith("please remember ") -> rawInput.substringAfter("please remember ", rawInput).trim()
                normalizedInput.startsWith("save this: ") -> rawInput.substringAfter("save this: ", rawInput).trim()
                else -> ""
            }

            if (fact.startsWith("that ", ignoreCase = true)) {
                fact = fact.substring(5).trim()
            }

            if (fact.isNotBlank()) {
                val category = determineCategory(fact)
                operations.add(
                    MemoryOperation.Store(
                        MemoryRecord(
                            category = category,
                            title = "Explicit Memory",
                            content = fact,
                            source = MemorySource.EXPLICIT_USER,
                            importance = MemoryImportance.NORMAL,
                            confidence = 1.0f
                        )
                    )
                )
            }
        }

        return operations
    }

    private fun determineCategory(content: String): MemoryCategory {
        val lower = content.lowercase(Locale.ROOT)
        return when {
            lower.contains("prefer") || lower.contains("favorite") || lower.contains("favourite") || lower.contains("like ") ->
                MemoryCategory.USER_PREFERENCE
            lower.contains("goal") || lower.contains("aim") || lower.contains("planning to") ->
                MemoryCategory.GOAL
            lower.contains("project") || lower.contains("building") || lower.contains("working on") ->
                MemoryCategory.PROJECT
            lower.contains("always") || lower.contains("routine") || lower.contains("every day") ->
                MemoryCategory.ROUTINE
            lower.contains("rule") || lower.contains("instruction") ->
                MemoryCategory.INSTRUCTION
            lower.contains("friend") || lower.contains("family") || lower.contains("wife") || lower.contains("husband") || lower.contains("colleague") ->
                MemoryCategory.RELATIONSHIP
            lower.contains("my name is") || lower.contains("i live in") || lower.contains("i am a") || lower.contains("i'm a") ->
                MemoryCategory.PERSONAL
            else ->
                MemoryCategory.FACT
        }
    }
}
