package ir.hrka.llm.runtime.api.exception

/**
 * Base exception for all LLM runtime errors.
 *
 * @param message Human-readable error description.
 * @param cause Optional underlying cause.
 */
open class LlmRuntimeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Thrown when [ir.hrka.llm.runtime.api.LlmRuntimeConfig.modelPath] does not point to an existing file.
 */
class LlmModelNotFoundException(
    modelPath: String,
    cause: Throwable? = null,
) : LlmRuntimeException("Model file not found: $modelPath", cause)

/**
 * Thrown when [ir.hrka.llm.runtime.api.LlmRuntimeConfig] contains invalid values.
 */
class LlmInvalidConfigException(
    message: String,
    cause: Throwable? = null,
) : LlmRuntimeException(message, cause)

/**
 * Thrown when model engine initialization fails.
 */
class LlmInitializationException(
    message: String,
    cause: Throwable? = null,
) : LlmRuntimeException(message, cause)

/**
 * Thrown when inference is attempted before successful initialization.
 */
class LlmNotInitializedException(
    message: String = "LLM runtime is not initialized. Call initialize() first.",
) : LlmRuntimeException(message)

/**
 * Thrown when an operation is attempted on a closed runtime.
 */
class LlmAlreadyClosedException(
    message: String = "LLM runtime is closed and cannot be used.",
) : LlmRuntimeException(message)

/**
 * Thrown when an inference request is invalid (e.g. empty input with no multimodal data).
 */
class LlmInvalidRequestException(
    message: String,
) : LlmRuntimeException(message)

/**
 * Thrown when inference fails during token generation.
 */
class LlmInferenceException(
    message: String,
    cause: Throwable? = null,
) : LlmRuntimeException(message, cause)

/**
 * Thrown when an operation is attempted while another inference is already running.
 */
class LlmGenerationInProgressException(
    message: String = "An inference request is already in progress.",
) : LlmRuntimeException(message)
