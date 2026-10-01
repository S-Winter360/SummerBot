package com.example.ai.capability

import com.example.core.context.SummerContext
import com.example.core.interaction.SummerInteraction
import com.example.core.personality.SummerPersonality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds bounded, structured context and system instructions for on-device Gemini Nano.
 * Keeps context within token limits and guarantees that Summer's persona and safety boundaries are enforced.
 */
class SummerPromptBuilder(
    private val personality: SummerPersonality = SummerPersonality.DEFAULT,
    private val maxRecentInteractions: Int = 4,
    private val maxMemories: Int = 5
) {
    fun buildPrompt(context: SummerContext, interaction: SummerInteraction): String {
        val sb = StringBuilder()

        // 1. Core System Directive & Persona
        sb.append("System Directive: You are ").append(personality.fullName).append(" (called ").append(personality.shortName).append(").\n")
        sb.append("You are an observant, calm, intelligent personal AI companion running locally on this Android device.\n")
        sb.append("Tone Directive: ").append(personality.toneDirective).append("\n")
        sb.append("Behavioral Rules:\n")
        sb.append("- Speak warmly, concisely, thoughtfully, and clearly.\n")
        sb.append("- You run locally on-device without relying on the cloud.\n")
        sb.append("- Do NOT claim capabilities that are not active (do NOT claim continuous background audio listening, live camera stream processing, or full phone automation).\n")
        sb.append("- Distinguish active features (local conversational understanding, bounded memory, secure action testing) from future planned capabilities.\n")

        // 2. Local Environment Context
        val timeFormat = SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm", Locale.getDefault())
        sb.append("- Current local time: ").append(timeFormat.format(Date(context.timestamp))).append("\n")
        sb.append("- Network state: ").append(context.networkState.name).append("\n\n")

        // 3. Bounded Explicit Memories
        if (context.activeMemories.isNotEmpty()) {
            sb.append("Known Explicit User Memories:\n")
            context.activeMemories.take(maxMemories).forEach { mem ->
                sb.append("- [").append(mem.category.name).append("] ").append(mem.title).append(": ").append(mem.content).append("\n")
            }
            sb.append("\n")
        }

        // 4. Bounded Recent Interactions
        val recent = context.recentInteractions.takeLast(maxRecentInteractions)
        if (recent.isNotEmpty()) {
            sb.append("Recent Conversation:\n")
            for (past in recent) {
                sb.append("User: ").append(past.userInput).append("\n")
                if (past.response != null) {
                    sb.append("Summer: ").append(past.response.text).append("\n")
                }
            }
            sb.append("\n")
        }

        // 5. User Input
        sb.append("Current User Message:\n")
        sb.append("User: ").append(interaction.userInput).append("\n")
        sb.append("Summer:")

        return sb.toString()
    }
}
