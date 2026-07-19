package ir.hrka.hooshmand.model

/**
 * A single published app version entry from the remote changelog.
 *
 * @property versionCode Android `versionCode` for this release.
 * @property versionName Human-readable version name (for example `"1.0.0"`).
 * @property changelog User-facing change notes for this release.
 */
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: List<String>,
)