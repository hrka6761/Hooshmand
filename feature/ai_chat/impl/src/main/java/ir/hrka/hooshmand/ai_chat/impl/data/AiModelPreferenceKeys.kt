package ir.hrka.hooshmand.ai_chat.impl.data

/**
 * Preference keys for AI chat model download state and user model settings.
 */
object AiModelPreferenceKeys {
    /** Whether a successful model download was recorded. */
    const val IS_MODEL_DOWNLOADED: String = "is_model_downloaded"

    /** Absolute path of the downloaded `.litertlm` model file. */
    const val MODEL_FILE_PATH: String = "model_file_path"

    /** Persisted [ir.hrka.llm.runtime.api.LlmAccelerator] name. */
    const val MODEL_ACCELERATOR: String = "model_accelerator"

    /** Persisted maximum context length in tokens. */
    const val MODEL_MAX_TOKENS: String = "model_max_tokens"

    /** Persisted TopK sampling value. */
    const val MODEL_TOP_K: String = "model_top_k"

    /** Persisted TopP sampling value. */
    const val MODEL_TOP_P: String = "model_top_p"

    /** Persisted sampling temperature. */
    const val MODEL_TEMPERATURE: String = "model_temperature"

    /** Persisted system instruction text; empty means none. */
    const val MODEL_SYSTEM_INSTRUCTION: String = "model_system_instruction"
}
