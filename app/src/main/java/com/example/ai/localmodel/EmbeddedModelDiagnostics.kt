package com.example.ai.localmodel

/**
 * Diagnostic summary for the embedded local model subsystem.
 * Exposes storage, RAM, installation state, backend, and progress for inspection in settings.
 */
data class EmbeddedModelDiagnostics(
    val modelId: EmbeddedModelId = EmbeddedModelId.GEMMA_3_1B_IT,
    val status: EmbeddedModelStatus = EmbeddedModelStatus.NOT_INSTALLED,
    val modelName: String = "Gemma 3 1B IT",
    val approximateSizeFormatted: String = "584 MB",
    val installedSizeBytes: Long = 0L,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytesToDownload: Long = 0L,
    val activeBackend: String = "CPU",
    val availableStorageBytes: Long = 0L,
    val totalDeviceRamBytes: Long = 0L,
    val isDeviceCompatible: Boolean = true,
    val compatibilityReason: String? = null,
    val lastError: String? = null,
    val localModelFilePath: String? = null
)
