package ir.hrka.hooshmand.ai_chat.impl

import ir.hrka.llm.runtime.api.LlmAccelerator
import ir.hrka.llm.runtime.api.LlmRuntimeConfig

/**
 * User-facing knobs for configuring the on-device AI chat model.
 *
 * Separates editable chat settings from [LlmRuntimeConfig], which also requires a resolved
 * [LlmRuntimeConfig.modelPath] and other runtime-only fields. Convert via [toRuntimeConfig]
 * when initializing [ir.hrka.llm.runtime.api.LlmRuntime].
 *
 * Defaults match [LlmRuntimeConfig] and the Gallery LLM chat config dialog.
 *
 * @property accelerator Compute backend used for text generation.
 * @property maxTokens Maximum context length (KV cache size) in tokens.
 * @property topK Top-K sampling parameter. Ignored on NPU/TPU backends.
 * @property topP Top-P (nucleus) sampling parameter. Ignored on NPU/TPU backends.
 * @property temperature Sampling temperature. Ignored on NPU/TPU backends.
 * @property systemInstruction Optional system prompt that guides model behavior.
 */
data class AiChatModelSettings(
    val accelerator: LlmAccelerator = LlmAccelerator.CPU,
    val maxTokens: Int = 1024,
    val topK: Int = 64,
    val topP: Float = 0.95f,
    val temperature: Float = 1.0f,
    val systemInstruction: String? = null,
) {

    /**
     * Builds a [LlmRuntimeConfig] for the given on-disk [modelPath].
     *
     * @param modelPath Absolute path to the downloaded `.litertlm` model file.
     * @return Runtime config ready for [ir.hrka.llm.runtime.api.LlmRuntime.initialize].
     */
    fun toRuntimeConfig(modelPath: String): LlmRuntimeConfig =
        LlmRuntimeConfig(
            modelPath = modelPath,
            accelerator = accelerator,
            maxTokens = maxTokens,
            topK = topK,
            topP = topP,
            temperature = temperature,
            systemInstruction = systemInstruction,
        )
}
