package ir.hrka.hooshmand.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network DTO for the decoded `changelog.json` payload.
 *
 * This is the JSON inside the GitHub Contents API Base64 [content] field,
 * not the Contents API envelope itself.
 *
 * @property latestVersion Highest published Android `versionCode`.
 * @property minimumVersion Lowest supported Android `versionCode`.
 * @property versions Published version entries.
 */
@Serializable
internal data class NetworkChangelog(
    @SerialName("latest_version") val latestVersion: Int,
    @SerialName("minimum_version") val minimumVersion: Int,
    val versions: List<NetworkVersionInfo>,
)

/**
 * Network DTO for one published version entry in `changelog.json`.
 *
 * @property versionCode Android `versionCode` for this release.
 * @property versionName Human-readable version name.
 * @property changelog User-facing change notes.
 */
@Serializable
internal data class NetworkVersionInfo(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    val changelog: List<String>,
)
