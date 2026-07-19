package ir.hrka.hooshmand.data.repository

import android.util.Base64
import ir.hrka.hooshmand.data.model.NetworkChangelog
import ir.hrka.hooshmand.data.model.asExternalModel
import ir.hrka.hooshmand.model.Changelog
import ir.hrka.hooshmand.network.ChangelogNetworkDataSource
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [ChangelogRepository] that reads `changelog.json` via GitHub Contents API.
 *
 * Flow: fetch envelope → Base64-decode `content` → parse payload JSON → map to [Changelog].
 */
@Singleton
internal class DefaultChangelogRepository @Inject constructor(
    private val networkDataSource: ChangelogNetworkDataSource,
    private val networkJson: Json,
) : ChangelogRepository {

    override suspend fun getChangelog(): Changelog {
        val fileContent = networkDataSource.getChangelogFile()
        val encodedContent = fileContent.content
            ?: throw IOException("GitHub Contents API response has no content for changelog.json")

        val decodedJson = decodeBase64Content(encodedContent)
        if (decodedJson.isBlank()) {
            throw IOException("Decoded changelog.json content is empty")
        }

        return networkJson
            .decodeFromString(NetworkChangelog.serializer(), decodedJson)
            .asExternalModel()
    }

    private fun decodeBase64Content(encodedContent: String): String {
        val decodedBytes = Base64.decode(encodedContent, Base64.DEFAULT)
        return String(decodedBytes, Charsets.UTF_8)
    }
}
