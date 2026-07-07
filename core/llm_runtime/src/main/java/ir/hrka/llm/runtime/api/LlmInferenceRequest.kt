package ir.hrka.llm.runtime.api

/**
 * Input for a single inference (generation) request.
 *
 * At least one of [prompt], [imageBytes], or [audioBytes] must be non-empty.
 *
 * @property prompt User text input.
 * @property imageBytes PNG-encoded image bytes for multimodal models.
 * @property audioBytes Raw audio bytes for multimodal models.
 * @property enableThinking When true, requests reasoning/thinking output if the model supports it.
 * @property extraContext Additional key-value context passed to the LiteRT-LM conversation.
 */
data class LlmInferenceRequest(
    val prompt: String = "",
    val imageBytes: List<ByteArray> = emptyList(),
    val audioBytes: List<ByteArray> = emptyList(),
    val enableThinking: Boolean = false,
    val extraContext: Map<String, String> = emptyMap(),
) {
    init {
        require(
            prompt.isNotBlank() || imageBytes.isNotEmpty() || audioBytes.isNotEmpty(),
        ) {
            "At least one of prompt, imageBytes, or audioBytes must be provided"
        }
    }
}
