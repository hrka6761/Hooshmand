package ir.hrka.hooshmand.data.model

import ir.hrka.hooshmand.model.Changelog
import ir.hrka.hooshmand.model.VersionInfo

/**
 * Maps [NetworkChangelog] to the shared domain [Changelog] model.
 */
internal fun NetworkChangelog.asExternalModel(): Changelog =
    Changelog(
        latestVersion = latestVersion,
        minimumVersion = minimumVersion,
        versions = versions.map(NetworkVersionInfo::asExternalModel),
    )

/**
 * Maps [NetworkVersionInfo] to the shared domain [VersionInfo] model.
 */
internal fun NetworkVersionInfo.asExternalModel(): VersionInfo =
    VersionInfo(
        versionCode = versionCode,
        versionName = versionName,
        changelog = changelog,
    )
