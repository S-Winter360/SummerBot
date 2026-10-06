package com.example.voice.engine

import com.example.voice.models.VoiceGender
import com.example.voice.models.VoiceProfile
import com.example.voice.models.VoiceProfileId
import java.util.Locale

data class ResolvedVoice(
    val profile: VoiceProfile,
    val engineVoiceId: String?,
    val isExactGenderMatch: Boolean,
    val isFallback: Boolean,
    val resolutionReason: String
)

class VoiceProfileResolver {

    /**
     * Resolves a target VoiceProfile against a list of available engine voice names/identifiers.
     * Guaranteed deterministic resolution and safe fallback behavior.
     */
    fun resolve(
        requestedProfile: VoiceProfile,
        availableEngineVoiceIds: List<String>
    ): ResolvedVoice {
        if (availableEngineVoiceIds.isEmpty()) {
            return ResolvedVoice(
                profile = requestedProfile.copy(engineVoiceId = null, isAvailable = false),
                engineVoiceId = null,
                isExactGenderMatch = false,
                isFallback = true,
                resolutionReason = "No engine voices reported; using default system audio fallback"
            )
        }

        val targetGender = requestedProfile.gender
        val isMale = (targetGender == VoiceGender.MALE || requestedProfile.id == VoiceProfileId.MALE)

        // 1. Direct engine voice ID match if specified in profile
        if (!requestedProfile.engineVoiceId.isNullOrBlank() &&
            availableEngineVoiceIds.contains(requestedProfile.engineVoiceId)
        ) {
            return ResolvedVoice(
                profile = requestedProfile,
                engineVoiceId = requestedProfile.engineVoiceId,
                isExactGenderMatch = true,
                isFallback = false,
                resolutionReason = "Exact engine voice ID match"
            )
        }

        // 2. Filter voices matching requested locale language (e.g. "en")
        val lang = requestedProfile.locale.language.lowercase(Locale.ROOT)
        val localeMatched = availableEngineVoiceIds.filter {
            it.lowercase(Locale.ROOT).startsWith(lang) || it.lowercase(Locale.ROOT).contains("-$lang")
        }

        val candidateList = if (localeMatched.isNotEmpty()) localeMatched else availableEngineVoiceIds

        // 3. Search for gender matching keywords
        // Ensure Google TTS and Android standard naming conventions are mapped accurately:
        // Female: 'sfg', 'iol', '-c-', '-f-', '-a-', 'female', 'woman'
        // Male: 'tpd', 'iom', '-d-', '-g-', '-b-', 'male' (not 'female')
        val exactMatch = candidateList.firstOrNull { voiceName ->
            val lower = voiceName.lowercase(Locale.ROOT)
            val isFemaleVoice = lower.contains("female") || lower.contains("woman") ||
                lower.contains("-f-") || lower.contains("-c-") || lower.contains("-a-") ||
                lower.contains("sfg") || lower.contains("iol") || lower.contains("#female")
            val isMaleVoice = (lower.contains("male") && !lower.contains("female")) ||
                lower.contains(" man") || lower.contains("-man") ||
                lower.contains("-g-") || lower.contains("-d-") || lower.contains("-b-") ||
                lower.contains("tpd") || lower.contains("iom") || lower.contains("#male")

            if (isMale) isMaleVoice && !isFemaleVoice else isFemaleVoice && !isMaleVoice
        }

        if (exactMatch != null) {
            return ResolvedVoice(
                profile = requestedProfile.copy(engineVoiceId = exactMatch, isAvailable = true),
                engineVoiceId = exactMatch,
                isExactGenderMatch = true,
                isFallback = false,
                resolutionReason = "Matched profile gender and language: $exactMatch"
            )
        }

        // 4. Fallback: if MALE is requested and multiple voices exist, avoid female-sounding first voice if possible
        val fallbackMatch = if (isMale && candidateList.size > 1) {
            val candidateNonFemale = candidateList.firstOrNull { voiceName ->
                val lower = voiceName.lowercase(Locale.ROOT)
                !lower.contains("female") && !lower.contains("sfg") && !lower.contains("-a-") && !lower.contains("-c-")
            }
            candidateNonFemale ?: candidateList.first()
        } else {
            candidateList.first()
        }

        return ResolvedVoice(
            profile = requestedProfile.copy(engineVoiceId = fallbackMatch, isAvailable = true),
            engineVoiceId = fallbackMatch,
            isExactGenderMatch = false,
            isFallback = true,
            resolutionReason = "Requested gender unavailable; gracefully fell back to $fallbackMatch"
        )
    }
}
