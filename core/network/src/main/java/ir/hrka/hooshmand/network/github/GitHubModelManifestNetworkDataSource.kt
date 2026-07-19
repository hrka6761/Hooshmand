package ir.hrka.hooshmand.network.github

import ir.hrka.hooshmand.network.ModelManifestNetworkDataSource
import ir.hrka.hooshmand.network.model.GitHubFileContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ModelManifestNetworkDataSource] backed by the public GitHub Contents API.
 *
 * No authentication token is sent; the `Hooshmand_App_Files` repository is public.
 */
@Singleton
internal class GitHubModelManifestNetworkDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val networkJson: Json,
) : ModelManifestNetworkDataSource {

    override suspend fun getModelManifestFile(): GitHubFileContent =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(MODEL_MANIFEST_CONTENTS_URL)
                .header(HEADER_ACCEPT, ACCEPT_GITHUB_V3_JSON)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException(
                        "GitHub Contents API failed for model.json: HTTP ${response.code}",
                    )
                }
                val body = response.body?.string()
                    ?: throw IOException(
                        "GitHub Contents API returned an empty body for model.json",
                    )
                networkJson.decodeFromString(GitHubFileContent.serializer(), body)
            }
        }

    private companion object {
        /**
         * Contents API path for the model catalog.
         *
         * Published in the repo as `models.json` (same schema as the app's `model.json` docs).
         */
        const val MODEL_MANIFEST_CONTENTS_URL =
            "https://api.github.com/repos/hrka6761/Hooshmand_App_Files/contents/models.json"
        const val HEADER_ACCEPT = "Accept"
        const val ACCEPT_GITHUB_V3_JSON = "application/vnd.github.v3+json"
    }
}
