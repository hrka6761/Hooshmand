package ir.hrka.llm.runtime.api

import ir.hrka.llm.runtime.api.exception.LlmRuntimeException

/**
 * Callback for streaming token generation results.
 *
 * All methods are invoked on the thread used by the underlying LiteRT-LM engine callback.
 * If updating UI, dispatch to the main thread from these callbacks.
 */
interface LlmGenerationCallback {
    /** Called for each streamed text token (partial result). */
    fun onToken(token: String)

    /** Called for each streamed thinking/reasoning token, if the model supports it. */
    fun onThinking(thinking: String) {}

    /** Called once when generation completes successfully. */
    fun onComplete()

    /** Called when generation fails. */
    fun onError(error: LlmRuntimeException)
}
