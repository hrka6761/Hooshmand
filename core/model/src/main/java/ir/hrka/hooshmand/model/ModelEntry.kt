package ir.hrka.hooshmand.model

/**
 * One on-device model entry from the remote model catalog.
 *
 * Download priority: use [singlePartAddress] when non-null/non-blank; otherwise download and
 * append [multiPartAddresses] into [modelName].
 *
 * @property modelName Output `.litertlm` file name after download/merge.
 * @property modelSize Expected final model size in bytes (from remote `model_size`).
 * @property singlePartAddress Direct URL for a single-file model, or `null` when unused.
 * @property multiPartAddresses Ordered part URLs appended into [modelName], or empty when unused.
 */
data class ModelEntry(
    val modelName: String,
    val modelSize: Long,
    val singlePartAddress: String?,
    val multiPartAddresses: List<String>,
)
