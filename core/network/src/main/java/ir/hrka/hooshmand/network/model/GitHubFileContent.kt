package ir.hrka.hooshmand.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for a GitHub Contents API file response.
 *
 * The [content] field is Base64-encoded file bytes when [encoding] is `"base64"`.
 * Deserialization should use `Json { ignoreUnknownKeys = true }` so unused envelope
 * fields (for example `_links`) do not fail parsing.
 *
 * @property name File name on GitHub.
 * @property path Path of the file within the repository.
 * @property sha Git blob SHA.
 * @property size File size in bytes.
 * @property url API URL for this content object.
 * @property htmlUrl Browser URL for this file.
 * @property gitUrl Git Data API URL for the blob.
 * @property downloadUrl Direct download URL for the raw file, if available.
 * @property type Content type (typically `"file"`).
 * @property content Base64-encoded file body, when present.
 * @property encoding Encoding of [content] (typically `"base64"`).
 */
@Serializable
data class GitHubFileContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val url: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("git_url") val gitUrl: String,
    @SerialName("download_url") val downloadUrl: String? = null,
    val type: String,
    val content: String? = null,
    val encoding: String? = null,
)
