package ir.hrka.hooshmand.data.model

import ir.hrka.hooshmand.model.ModelEntry
import ir.hrka.hooshmand.model.ModelManifest

/**
 * Maps [NetworkModelManifest] to the shared domain [ModelManifest] model.
 */
internal fun NetworkModelManifest.asExternalModel(): ModelManifest =
    ModelManifest(
        models = models.map(NetworkModelEntry::asExternalModel),
    )

/**
 * Maps [NetworkModelEntry] to the shared domain [ModelEntry] model.
 */
internal fun NetworkModelEntry.asExternalModel(): ModelEntry =
    ModelEntry(
        modelName = modelName,
        modelSize = modelSize,
        singlePartAddress = singlePartAddress,
        multiPartAddresses = multiPartAddress,
    )
