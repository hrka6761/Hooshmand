package ir.hrka.hooshmand.network

import ir.hrka.hooshmand.network.model.GitHubFileContent

/**
 * Remote data source that fetches the model catalog file envelope from the network.
 */
interface ModelManifestNetworkDataSource {

    /**
     * Fetches the GitHub Contents API response for `model.json`.
     *
     * @return Parsed [GitHubFileContent] whose [GitHubFileContent.content] is Base64-encoded.
     * @throws java.io.IOException when the HTTP call fails or the response body is empty.
     * @throws kotlinx.serialization.SerializationException when the response JSON is invalid.
     */
    suspend fun getModelManifestFile(): GitHubFileContent
}
