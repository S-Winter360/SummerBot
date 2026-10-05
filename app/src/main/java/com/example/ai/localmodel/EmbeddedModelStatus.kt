package com.example.ai.localmodel

/**
 * Explicit lifecycle status for locally managed embedded language model files.
 * The application never reports a model as READY unless validated on disk.
 */
enum class EmbeddedModelStatus(val label: String) {
    NOT_INSTALLED("Not installed"),
    CHECKING("Checking"),
    IMPORTING("Importing"),
    DOWNLOADING("Downloading"),
    VERIFYING("Verifying"),
    READY("Ready"),
    INITIALIZING("Initializing"),
    RUNNING("Running"),
    ERROR("Error"),
    UNAVAILABLE("Unavailable"),
    INSUFFICIENT_STORAGE("Insufficient storage"),
    INCOMPATIBLE_DEVICE("Incompatible device"),
    CORRUPTED("Corrupted"),
    CANCELLED("Cancelled");

    val isAvailable: Boolean get() = this == READY || this == RUNNING
}
