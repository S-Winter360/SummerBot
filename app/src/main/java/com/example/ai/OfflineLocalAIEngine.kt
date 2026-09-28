package com.example.ai

import com.example.actions.ActionRequest
import com.example.ai.models.AIModelInfo
import com.example.ai.models.AIRequest
import com.example.ai.models.AIResponse
import com.example.ai.models.RecognizedIntent
import com.example.core.personality.SummerPersonality
import com.example.security.Capability

/**
 * Concrete local offline reasoning engine for Summer's architectural foundation phase.
 * Provides transparent, honest execution without fake AI simulations or cloud dependencies.
 */
class OfflineLocalAIEngine(
    private val personality: SummerPersonality = SummerPersonality.DEFAULT
) : AIEngine {

    override val modelInfo: AIModelInfo = AIModelInfo(
        name = "Summer Offline Core",
        version = "0.1.0-alpha",
        isLocalOffline = true,
        description = "On-device architecture router and intent dispatcher."
    )

    override val isReady: Boolean = true

    override suspend fun evaluateIntent(input: String): RecognizedIntent {
        val normalized = input.trim().lowercase()

        return when {
            normalized.contains("status") || normalized.contains("health") || normalized.contains("system") -> {
                RecognizedIntent.SystemStatus(input)
            }

            normalized.contains("who are you") || normalized.contains("personality") || normalized.contains("traits") -> {
                RecognizedIntent.PersonalityQuery(input)
            }

            normalized.contains("test network") || normalized.contains("connect internet") -> {
                RecognizedIntent.CapabilityRequest(
                    ActionRequest(
                        capability = Capability.INTERNET,
                        actionName = "Check External Connectivity",
                        reasoning = "User requested verification of external network access via capability gate."
                    )
                )
            }

            normalized.contains("test mic") || normalized.contains("listen") || normalized.contains("audio") -> {
                RecognizedIntent.CapabilityRequest(
                    ActionRequest(
                        capability = Capability.MICROPHONE,
                        actionName = "Activate Audio Stream",
                        reasoning = "User initiated request to test microphone capture authorization."
                    )
                )
            }

            normalized.contains("test camera") || normalized.contains("see") || normalized.contains("vision") -> {
                RecognizedIntent.CapabilityRequest(
                    ActionRequest(
                        capability = Capability.CAMERA,
                        actionName = "Analyze Visual Frame",
                        reasoning = "User initiated request to test vision sensor authorization."
                    )
                )
            }

            normalized.contains("remember") || normalized.contains("save memory") || normalized.contains("note") -> {
                RecognizedIntent.CapabilityRequest(
                    ActionRequest(
                        capability = Capability.LOCAL_MEMORY_WRITE,
                        actionName = "Persist Memory Record",
                        parameters = mapOf("content" to input),
                        reasoning = "User requested storing data into local memory repository."
                    )
                )
            }

            else -> {
                RecognizedIntent.GeneralConversation(input)
            }
        }
    }

    override suspend fun processQuery(request: AIRequest): AIResponse {
        val intent = evaluateIntent(request.query)

        val (responseText, actionRequest) = when (intent) {
            is RecognizedIntent.SystemStatus -> {
                val statusText = "System operational. Summer is active in offline mode. Core state engine, local Room memory, and capability gatekeeper are functioning normally."
                Pair(statusText, null)
            }

            is RecognizedIntent.PersonalityQuery -> {
                val traitList = personality.traits.take(4).joinToString(", ") { it.label }
                val text = "I am ${personality.name} (${personality.shortName}), your ${personality.role}. My initial behavioral characteristics are $traitList. I operate strictly offline-first to protect your privacy."
                Pair(text, null)
            }

            is RecognizedIntent.CapabilityRequest -> {
                val action = intent.actionRequest
                val text = "Routing action request [${action.actionName}] through Summer's security authorization gatekeeper."
                Pair(text, action)
            }

            is RecognizedIntent.GeneralConversation -> {
                val text = "I received your query. In this architectural foundation phase, local model execution pipelines and state transitions are verified. Complex on-device weights will be integrated in subsequent phases."
                Pair(text, null)
            }
        }

        return AIResponse(
            text = responseText,
            modelUsed = modelInfo.name,
            recognizedIntent = intent,
            requiredAction = actionRequest
        )
    }
}
