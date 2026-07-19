package ir.hrka.hooshmand.data.repository

import android.util.Base64
import ir.hrka.hooshmand.data.model.NetworkModelManifest
import ir.hrka.hooshmand.data.model.asExternalModel
import ir.hrka.hooshmand.model.ModelEntry
import ir.hrka.hooshmand.model.ModelManifest
import ir.hrka.hooshmand.network.ModelManifestNetworkDataSource
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [ModelManifestRepository] that reads `model.json` via GitHub Contents API.
 *
 * Flow: fetch envelope → Base64-decode `content` → parse payload JSON → map to [ModelManifest].
 */
@Singleton
internal class DefaultModelManifestRepository @Inject constructor(
    private val networkDataSource: ModelManifestNetworkDataSource,
    private val networkJson: Json,
) : ModelManifestRepository {

    override suspend fun getModelManifest(): ModelManifest {
        val fileContent = networkDataSource.getModelManifestFile()
        val encodedContent = fileContent.content
            ?: throw IOException("GitHub Contents API response has no content for model.json")

        val decodedJson = decodeBase64Content(encodedContent)
        if (decodedJson.isBlank()) {
            throw IOException("Decoded model.json content is empty")
        }

        return networkJson
            .decodeFromString(NetworkModelManifest.serializer(), decodedJson)
            .asExternalModel()
    }

    override suspend fun getLatestModel(): ModelEntry =
        getModelManifest().latestModel()

    private fun decodeBase64Content(encodedContent: String): String {
        val decodedBytes = Base64.decode(encodedContent, Base64.DEFAULT)
        return String(decodedBytes, Charsets.UTF_8)
    }
}
