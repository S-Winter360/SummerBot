package com.example.ai.capability

enum class AICapability(val title: String, val description: String) {
    TEXT_GENERATION(
        title = "Text Generation",
        description = "Generating natural language responses and completions."
    ),
    CHAT(
        title = "Chat",
        description = "Context-aware multi-turn conversational intelligence."
    ),
    STRUCTURED_OUTPUT(
        title = "Structured Output",
        description = "Synthesizing structured data, action proposals, and typed intents."
    ),
    IMAGE_UNDERSTANDING(
        title = "Image Understanding",
        description = "Reasoning over camera frames and visual scenes."
    ),
    SPEECH_RECOGNITION(
        title = "Speech Recognition",
        description = "Converting audio signals into structured text tokens."
    ),
    SUMMARIZATION(
        title = "Summarization",
        description = "Extracting concise summaries and key facts from conversation."
    )
}
