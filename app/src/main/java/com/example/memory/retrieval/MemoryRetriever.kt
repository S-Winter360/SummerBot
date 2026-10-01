package com.example.memory.retrieval

import com.example.memory.models.MemoryCategory
import com.example.memory.models.MemoryImportance
import com.example.memory.models.MemoryRecord
import java.util.Locale

data class ScoredMemory(
    val memory: MemoryRecord,
    val score: Float,
    val matchReasons: List<String> = emptyList()
)

interface MemoryRetriever {
    suspend fun retrieveRelevantMemories(
        query: String,
        allMemories: List<MemoryRecord>,
        maxResults: Int = 5,
        minScoreThreshold: Float = 0.15f
    ): List<MemoryRecord>

    fun calculateRelevance(query: String, memory: MemoryRecord): Float
}

class DefaultMemoryRetriever(
    private val maxContentLength: Int = 200
) : MemoryRetriever {

    private val stopWords = setOf(
        "the", "a", "an", "is", "are", "was", "were", "of", "and", "or", "in",
        "to", "for", "on", "with", "at", "by", "from", "it", "this", "that",
        "what", "who", "where", "when", "why", "how", "do", "does", "did",
        "i", "my", "me", "you", "your", "we", "our", "tell", "show", "can",
        "could", "would", "please", "about"
    )

    override suspend fun retrieveRelevantMemories(
        query: String,
        allMemories: List<MemoryRecord>,
        maxResults: Int,
        minScoreThreshold: Float
    ): List<MemoryRecord> {
        val activeMemories = allMemories.filter { it.isActive && it.content.isNotBlank() }
        if (activeMemories.isEmpty()) return emptyList()

        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isBlank()) {
            return activeMemories
                .sortedWith(compareByDescending<MemoryRecord> { it.importance.ordinal }.thenByDescending { it.updatedAt })
                .take(maxResults)
                .map { sanitizeMemoryLength(it) }
        }

        val scored = activeMemories.map { memory ->
            val score = calculateRelevance(normalizedQuery, memory)
            ScoredMemory(memory, score)
        }

        return scored
            .filter { it.score >= minScoreThreshold }
            .sortedByDescending { it.score }
            .take(maxResults)
            .map { sanitizeMemoryLength(it.memory) }
    }

    override fun calculateRelevance(query: String, memory: MemoryRecord): Float {
        val queryTokens = extractTokens(query)
        val contentTokens = extractTokens(memory.content)
        val titleTokens = extractTokens(memory.title)

        var matchScore = 0.0f

        // 1. Lexical token overlap
        if (queryTokens.isNotEmpty()) {
            val allMemoryTokens = contentTokens + titleTokens
            val matchedTokens = queryTokens.filter { qToken ->
                allMemoryTokens.any { mToken -> mToken == qToken || mToken.contains(qToken) || qToken.contains(mToken) }
            }

            val tokenMatchRatio = matchedTokens.size.toFloat() / queryTokens.size.toFloat()
            matchScore += tokenMatchRatio * 0.60f

            // Direct substring matches
            val lowerContent = memory.content.lowercase(Locale.ROOT)
            val lowerTitle = memory.title.lowercase(Locale.ROOT)
            for (qToken in queryTokens) {
                if (lowerContent.contains(qToken)) matchScore += 0.15f
                if (lowerTitle.contains(qToken)) matchScore += 0.20f
            }
        }

        // 2. Category query intent alignment
        val lowerQuery = query.lowercase(Locale.ROOT)
        when (memory.category) {
            MemoryCategory.PREFERENCE, MemoryCategory.USER_PREFERENCE -> {
                if (lowerQuery.contains("prefer") || lowerQuery.contains("like") || lowerQuery.contains("favorite") || lowerQuery.contains("favourite")) {
                    matchScore += 0.30f
                }
            }
            MemoryCategory.PERSONAL -> {
                if (lowerQuery.contains("who am i") || lowerQuery.contains("my name") || lowerQuery.contains("about me") || lowerQuery.contains("where do i")) {
                    matchScore += 0.30f
                }
            }
            MemoryCategory.GOAL, MemoryCategory.PROJECT -> {
                if (lowerQuery.contains("goal") || lowerQuery.contains("project") || lowerQuery.contains("building") || lowerQuery.contains("working on")) {
                    matchScore += 0.30f
                }
            }
            MemoryCategory.INSTRUCTION, MemoryCategory.ROUTINE -> {
                if (lowerQuery.contains("routine") || lowerQuery.contains("always") || lowerQuery.contains("how should you") || lowerQuery.contains("rule")) {
                    matchScore += 0.25f
                }
            }
            else -> {}
        }

        // If there is zero query match, do not return irrelevant memories
        if (matchScore == 0.0f) {
            return 0.0f
        }

        var totalScore = matchScore

        // 3. Importance weighting
        val importanceBoost = when (memory.importance) {
            MemoryImportance.CRITICAL -> 0.30f
            MemoryImportance.HIGH -> 0.20f
            MemoryImportance.NORMAL -> 0.10f
            MemoryImportance.LOW -> 0.02f
        }
        totalScore += importanceBoost

        // 4. Confidence weighting
        val clampedConfidence = memory.confidence.coerceIn(0.0f, 1.0f)
        totalScore += (clampedConfidence * 0.15f)

        // 5. Recency boost (within last 7 days)
        val ageDays = (System.currentTimeMillis() - memory.updatedAt).coerceAtLeast(0L) / (1000 * 60 * 60 * 24)
        if (ageDays < 7) {
            totalScore += 0.08f
        }

        return totalScore.coerceIn(0.0f, 2.0f)
    }

    private fun extractTokens(text: String): Set<String> {
        return text.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length >= 2 && !stopWords.contains(it) }
            .toSet()
    }

    private fun sanitizeMemoryLength(record: MemoryRecord): MemoryRecord {
        if (record.content.length <= maxContentLength) return record
        val truncated = record.content.take(maxContentLength - 3).trimEnd() + "..."
        return record.copy(content = truncated)
    }
}
