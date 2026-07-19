package ir.hrka.llm.runtime.api

import kotlinx.coroutines.flow.Flow

/**
 * Main interface for on-device LLM inference.
 *
 * Thread safety: only one inference request may run at a time. [initialize] and [close]
 * must not be called concurrently with [generate] or [generateFlow].
 *
 * Typical lifecycle:
 * 1. [initialize] — load model from disk into memory
 * 2. [generate] or [generateFlow] — run inference (repeatable)
 * 3. [resetConversation] — clear chat history (optional)
 * 4. [close] — release native resources
 */
interface LlmRuntime {
    /** Current lifecycle state. */
    val state: LlmRuntimeState

    /**
     * Loads the model and prepares a conversation session.
     *
     * If already initialized, the previous session is closed before re-initializing.
     *
     * @param config Model path, accelerator, and sampler settings.
     * @throws ir.hrka.llm.runtime.api.exception.LlmModelNotFoundException if the model file is missing.
     * @throws ir.hrka.llm.runtime.api.exception.LlmInitializationException if engine loading fails.
     * @throws ir.hrka.llm.runtime.api.exception.LlmAlreadyClosedException if [close] was called.
     */
    suspend fun initialize(config: LlmRuntimeConfig)

    /**
     * Runs inference and streams results through [callback].
     *
     * @param request User input (text and/or multimodal data).
     * @param callback Receives streaming tokens and completion/error signals.
     * @throws ir.hrka.llm.runtime.api.exception.LlmNotInitializedException if [initialize] was not called.
     * @throws ir.hrka.llm.runtime.api.exception.LlmGenerationInProgressException if already generating.
     * @throws ir.hrka.llm.runtime.api.exception.LlmInvalidRequestException if [request] is invalid.
     */
    suspend fun generate(request: LlmInferenceRequest, callback: LlmGenerationCallback)

    /**
     * Runs inference and returns a [Flow] of [LlmGenerationEvent]s.
     *
     * Cancelling the flow collection calls [stopGeneration] automatically.
     */
    fun generateFlow(request: LlmInferenceRequest): Flow<LlmGenerationEvent>

    /**
     * Cancels the current inference request, if any.
     *
     * Safe to call when no generation is in progress.
     */
    fun stopGeneration()

    /**
     * Resets the conversation history while keeping the loaded model in memory.
     *
     * @param systemInstruction Optional new system instruction. Uses the config from [initialize]
     * if null.
     * @param initialMessages Prior turns to restore into the new session (for example after
     * reopening a saved chat). Empty starts a blank conversation.
     */
    suspend fun resetConversation(
        systemInstruction: String? = null,
        initialMessages: List<LlmHistoryMessage> = emptyList(),
    )

    /**
     * Releases all native resources (engine, conversation).
     *
     * After calling this, the instance must not be reused.
     */
    suspend fun close()
}
