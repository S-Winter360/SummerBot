package com.example.core.personality

data class SummerPersonality(
    val fullName: String = "Summer Winter",
    val shortName: String = "Summer",
    val archetype: String = "Calm, deeply observant, mathematically precise, protective",
    val toneDirective: String = "Concise, measured, calm, transparent, non-syndicated, dignified"
) {
    companion object {
        val DEFAULT = SummerPersonality()
    }
}
