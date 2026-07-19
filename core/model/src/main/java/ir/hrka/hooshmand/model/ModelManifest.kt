package ir.hrka.hooshmand.model

/**
 * Remote model catalog from `model.json`.
 *
 * @property models Published model entries; the last entry is the one to download.
 */
data class ModelManifest(
    val models: List<ModelEntry>,
) {

    /**
     * Returns the latest published model (last entry in [models]).
     *
     * @throws IllegalStateException when [models] is empty.
     */
    fun latestModel(): ModelEntry =
        models.lastOrNull()
            ?: throw IllegalStateException("model.json contains no models")
}
