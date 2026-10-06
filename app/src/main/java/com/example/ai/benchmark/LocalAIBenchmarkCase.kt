package com.example.ai.benchmark

/**
 * Benchmark capability category.
 */
enum class BenchmarkCategory(val label: String) {
    FACTUAL("Factual Question"),
    CONVERSATIONAL("Conversational Response"),
    EXPLANATION("Basic Explanation"),
    CREATIVE("Creative Generation"),
    ORCHESTRATION_OVERHEAD("Architectural Context Overhead")
}

/**
 * Standardized controlled benchmark test case for local embedded generative AI.
 */
data class LocalAIBenchmarkCase(
    val id: String,
    val title: String,
    val prompt: String,
    val category: BenchmarkCategory,
    val expectedCapability: String,
    val systemInstruction: String? = null
) {
    companion object {
        /**
         * The standard controlled benchmark suite prescribed by Phase 0J-R1.
         */
        val SUITE: List<LocalAIBenchmarkCase> = listOf(
            LocalAIBenchmarkCase(
                id = "TEST_A",
                title = "Test A — Simple Factual Question",
                prompt = "What is the capital of Ghana?",
                category = BenchmarkCategory.FACTUAL,
                expectedCapability = "Simple factual generation; short concise response."
            ),
            LocalAIBenchmarkCase(
                id = "TEST_B",
                title = "Test B — Simple Conversational Response",
                prompt = "I'm tired today.",
                category = BenchmarkCategory.CONVERSATIONAL,
                expectedCapability = "Natural conversational response; no factual lookup; no identity introduction."
            ),
            LocalAIBenchmarkCase(
                id = "TEST_C",
                title = "Test C — Basic Explanation",
                prompt = "Explain what Python programming is in simple terms.",
                category = BenchmarkCategory.EXPLANATION,
                expectedCapability = "Explanation; basic instruction following."
            ),
            LocalAIBenchmarkCase(
                id = "TEST_D",
                title = "Test D — Creative Generation",
                prompt = "Tell me a short story about a young boy in Ghana who discovers something unexpected while walking home from school.",
                category = BenchmarkCategory.CREATIVE,
                expectedCapability = "Creative narrative generation; coherent storytelling."
            ),
            LocalAIBenchmarkCase(
                id = "TEST_E",
                title = "Test E — Context & Prompt Overhead",
                prompt = "What is the capital of Ghana?",
                category = BenchmarkCategory.ORCHESTRATION_OVERHEAD,
                expectedCapability = "Direct comparison with Summer system instructions applied to evaluate context impact on latency.",
                systemInstruction = "You are Summer, a calm, observant personal companion. You provide concise, direct, helpful answers without preambles."
            )
        )
    }
}
