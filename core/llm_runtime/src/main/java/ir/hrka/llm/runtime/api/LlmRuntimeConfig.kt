package ir.hrka.llm.runtime.api

/**
 * Configuration for loading and running an on-device LLM.
 *
 * @property modelPath Absolute path to the `.litertlm` model file on local storage.
 * @property accelerator Primary compute backend for text generation.
 * @property visionAccelerator Backend for image encoding when [supportImage] is true. Defaults to [accelerator].
 * @property maxTokens Maximum context length (KV cache size) in tokens.
 * @property topK Top-K sampling parameter. Ignored on NPU/TPU backends.
 * @property topP Top-P (nucleus) sampling parameter. Ignored on NPU/TPU backends.
 * @property temperature Sampling temperature. Ignored on NPU/TPU backends.
 * @property systemInstruction Optional system prompt guiding model behavior.
 * @property supportImage Enable vision input in the conversation. Requires a multimodal model.
 * @property supportAudio Enable audio input in the conversation. Requires a multimodal model.
 * @property cacheDir Optional cache directory for models loaded from temporary paths (e.g. `/data/local/tmp`).
 * @property enableSpeculativeDecoding Enable speculative decoding if the model supports it.
 */
data class LlmRuntimeConfig(
    val modelPath: String,
    val accelerator: LlmAccelerator = LlmAccelerator.GPU,
    val visionAccelerator: LlmAccelerator? = null,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Float = DEFAULT_TOP_P,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val systemInstruction: String? = null,
    val supportImage: Boolean = false,
    val supportAudio: Boolean = false,
    val cacheDir: String? = null,
    val enableSpeculativeDecoding: Boolean = false,
) {
    init {
        require(modelPath.isNotBlank()) { "modelPath must not be blank" }
        require(maxTokens > 0) { "maxTokens must be positive, got $maxTokens" }
        require(topK > 0) { "topK must be positive, got $topK" }
        require(topP in 0.0f..1.0f) { "topP must be in [0, 1], got $topP" }
        require(temperature >= 0.0f) { "temperature must be non-negative, got $temperature" }
    }

    companion object {
        const val DEFAULT_MAX_TOKENS = 1024
        const val DEFAULT_TOP_K = 64
        const val DEFAULT_TOP_P = 0.95f
        const val DEFAULT_TEMPERATURE = 1.0f
    }
}
