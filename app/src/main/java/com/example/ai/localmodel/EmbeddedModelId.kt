package com.example.ai.localmodel

/**
 * Identifiers and specifications for embedded local generative language models supported by Summer.
 */
enum class EmbeddedModelId(
    val modelName: String,
    val description: String,
    val approximateSizeBytes: Long,
    val defaultFilename: String,
    val expectedExtension: String = ".litertlm"
) {
    GEMMA_3_1B_IT(
        modelName = "Gemma 3 1B IT",
        description = "4-bit quantized LiteRT-LM conversational model for on-device local inference",
        approximateSizeBytes = 584L * 1024L * 1024L, // ~584 MB
        defaultFilename = "gemma-3-1b-it-cpu.litertlm",
        expectedExtension = ".litertlm"
    )
}
