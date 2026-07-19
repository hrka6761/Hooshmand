package ir.hrka.hooshmand.model

/**
 * Remote app changelog used to decide whether an update is available.
 *
 * Sourced from `changelog.json` on GitHub.
 *
 * @property latestVersion Highest published Android `versionCode`.
 * @property minimumVersion Lowest `versionCode` still allowed to run without a mandatory update.
 * @property versions Chronological list of published version entries.
 */
data class Changelog(
    val latestVersion: Int,
    val minimumVersion: Int,
    val versions: List<VersionInfo>,
)