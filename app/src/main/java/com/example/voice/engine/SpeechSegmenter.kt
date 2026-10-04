package com.example.voice.engine

/**
 * Splits response text into natural, bounded conversational speech chunks.
 * Avoids sending massive text blocks to TTS at once, prevents word clipping,
 * and normalizes formatting for clean speech synthesis.
 */
class SpeechSegmenter(
    private val maxChunkLength: Int = 200
) {
    fun segment(text: String): List<String> {
        val cleanedText = cleanFormatting(text)
        if (cleanedText.isBlank()) return emptyList()

        if (cleanedText.length <= maxChunkLength && !cleanedText.contains("\n")) {
            return listOf(cleanedText)
        }

        val rawSentences = splitIntoSentences(cleanedText)
        val chunks = mutableListOf<String>()

        for (sentence in rawSentences) {
            val trimmed = sentence.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.length <= maxChunkLength) {
                chunks.add(trimmed)
            } else {
                // Split long sentence at punctuation or space boundaries
                chunks.addAll(splitLongSentence(trimmed, maxChunkLength))
            }
        }

        return chunks
    }

    private fun cleanFormatting(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), "Code snippet omitted.") // omit large raw code blocks in speech
            .replace(Regex("[#*_`~]"), "") // strip markdown styling marks
            .replace(Regex("\\[.*?\\]\\(.*?\\)"), "") // strip markdown links
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Splits by sentence terminal characters followed by whitespace, or line breaks
        val pattern = Regex("(?<=[.!?])\\s+|\\n+")
        return text.split(pattern).filter { it.isNotBlank() }
    }

    private fun splitLongSentence(sentence: String, maxLen: Int): List<String> {
        val result = mutableListOf<String>()
        var remaining = sentence

        while (remaining.length > maxLen) {
            // Find secondary punctuation boundary (, ; :) within window
            val window = remaining.substring(0, maxLen)
            var splitIndex = window.lastIndexOfAny(charArrayOf(',', ';', ':', '-'))

            if (splitIndex < maxLen / 3) {
                // If no good punctuation, split at last space
                splitIndex = window.lastIndexOf(' ')
            }

            if (splitIndex <= 0) {
                // Hard fallback to maxLen if no whitespace exists
                splitIndex = maxLen
            }

            val chunk = remaining.substring(0, splitIndex).trim()
            if (chunk.isNotEmpty()) {
                result.add(chunk)
            }
            remaining = remaining.substring(splitIndex).trim()
        }

        if (remaining.isNotEmpty()) {
            result.add(remaining)
        }

        return result
    }
}
