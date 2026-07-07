package ir.hrka.llm.runtime.api

/**
 * Lifecycle state of an [LlmRuntime] instance.
 */
enum class LlmRuntimeState {
    /** Runtime created but [LlmRuntime.initialize] has not been called. */
    IDLE,

    /** [LlmRuntime.initialize] is in progress. */
    INITIALIZING,

    /** Model loaded and ready for inference. */
    READY,

    /** An inference request is currently running. */
    GENERATING,

    /** [LlmRuntime.close] was called; the instance must not be reused. */
    CLOSED,
}
