package com.example.vision

/**
 * Strongly-typed lifecycle state for local vision processing pipelines.
 * Independent from the authoritative global [com.example.core.state.SummerState].
 */
enum class VisionState {
    IDLE,
    INITIALIZING,
    READY,
    OBSERVING,
    PROCESSING,
    COMPLETED,
    ERROR,
    STOPPED
}
