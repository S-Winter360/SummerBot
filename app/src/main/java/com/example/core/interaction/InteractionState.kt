package com.example.core.interaction

/**
 * Formal lifecycle states for an individual interaction.
 * Distinct from high-level assistant [com.example.core.state.SummerState].
 */
enum class InteractionState {
    /** Interaction received from input channel. */
    RECEIVED,

    /** Analyzing natural language or input stream to extract intent and entities. */
    UNDERSTANDING,

    /** Reasoning over context, memory, and cognitive policies. */
    REASONING,

    /** Preparing and generating verbal / textual response. */
    RESPONDING,

    /** Executing an authorized device action pipeline. */
    EXECUTING,

    /** Interaction completed successfully. */
    COMPLETED,

    /** Interaction terminated due to fault or policy rejection. */
    FAILED
}
