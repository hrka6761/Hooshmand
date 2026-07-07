package ir.hrka.llm.runtime.api

import ir.hrka.llm.runtime.api.exception.LlmRuntimeException

/**
 * Events emitted during streaming inference via [LlmRuntime.generateFlow].
 */
sealed interface LlmGenerationEvent {
    /** A streamed text token. */
    data class Token(val text: String) : LlmGenerationEvent

    /** A streamed thinking/reasoning token. */
    data class Thinking(val text: String) : LlmGenerationEvent

    /** Generation completed successfully. */
    data object Done : LlmGenerationEvent

    /** Generation failed. */
    data class Error(val exception: LlmRuntimeException) : LlmGenerationEvent
}
