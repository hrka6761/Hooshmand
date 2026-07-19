package ir.hrka.hooshmand.data.repository

import ir.hrka.hooshmand.model.ModelEntry
import ir.hrka.hooshmand.model.ModelManifest

/**
 * Repository for the remote on-device model catalog (`model.json`).
 */
interface ModelManifestRepository {

    /**
     * Fetches and decodes the remote model catalog.
     *
     * @return Domain [ModelManifest] parsed from GitHub `model.json`.
     * @throws java.io.IOException when the network call fails or content cannot be decoded.
     * @throws kotlinx.serialization.SerializationException when JSON parsing fails.
     */
    suspend fun getModelManifest(): ModelManifest

    /**
     * Fetches the catalog and returns the latest model (last entry in `models`).
     *
     * @return Latest [ModelEntry] to download.
     * @throws java.io.IOException when the network call fails or content cannot be decoded.
     * @throws IllegalStateException when the catalog has no models.
     * @throws kotlinx.serialization.SerializationException when JSON parsing fails.
     */
    suspend fun getLatestModel(): ModelEntry
}
