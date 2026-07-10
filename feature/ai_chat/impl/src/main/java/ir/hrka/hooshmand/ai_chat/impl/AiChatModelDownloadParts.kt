package ir.hrka.hooshmand.ai_chat.impl

/**
 * Download URLs and metadata for the on-device AI chat model.
 *
 * The model is delivered as multiple parts that are appended into a single `.litertlm` file.
 * Update the [parts] list when the hosted model or CDN endpoints change.
 */
object AiChatModelDownloadParts {

    /** Output file name after all parts are merged. */
    const val MODEL_FILE_NAME: String = "gemma-3n-E4B-it-int4.litertlm"

    /** Subdirectory under the chosen storage location. */
    const val MODEL_DIRECTORY: String = "hooshmand_app_ai_models"

    /**
     * A single downloadable segment of the model archive.
     *
     * @property url Direct HTTP(S) URL of the part.
     * @property sizeInBytes Expected size in bytes, used for progress reporting; null when unknown.
     */
    data class Part(
        val url: String,
        val sizeInBytes: Long? = null,
    )

    /**
     * Ordered model parts. Parts are downloaded sequentially and appended into [MODEL_FILE_NAME].
     */
    val parts: List<Part> =
        listOf(
            Part(
                url = "https://media.githubusercontent.com/media/hrka6761/AI-Models/refs/heads/main/gemma-3n-E4B-it-int4.litertlm_000?download=true",
            ),
            Part(
                url = "https://media.githubusercontent.com/media/hrka6761/AI-Models/refs/heads/main/gemma-3n-E4B-it-int4.litertlm_001?download=true",
            ),
            Part(
                url = "https://media.githubusercontent.com/media/hrka6761/AI-Models/refs/heads/main/gemma-3n-E4B-it-int4.litertlm_002?download=true",
            ),
            Part(
                url = "https://media.githubusercontent.com/media/hrka6761/AI-Models/refs/heads/main/gemma-3n-E4B-it-int4.litertlm_003?download=true",
            ),
            Part(
                url = "https://media.githubusercontent.com/media/hrka6761/AI-Models/refs/heads/main/gemma-3n-E4B-it-int4.litertlm_004?download=true",
            ),
        )

    /** Sum of all part sizes when every [Part.sizeInBytes] is known; null otherwise. */
    val totalSizeInBytes: Long = 4_919_541_760L
}