package com.example.ai.localmodel

/**
 * Metadata record for an embedded local AI model.
 */
data class EmbeddedModelMetadata(
    val id: EmbeddedModelId = EmbeddedModelId.GEMMA_3_1B_IT,
    val modelName: String = id.modelName,
    val filename: String = id.defaultFilename,
    val sizeBytes: Long = id.approximateSizeBytes,
    val installedSizeBytes: Long? = null,
    val sha256Checksum: String? = null,
    val backend: String = "CPU",
    val runtime: String = id.runtime,
    val quantization: String = id.quantization,
    val isQuantized: Boolean = true,
    val maxTokens: Int = 1024,
    val installedTimestamp: Long? = null
)
