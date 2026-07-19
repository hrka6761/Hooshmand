package ir.hrka.hooshmand.domain

import ir.hrka.hooshmand.model.Changelog

/**
 * Result of comparing the installed app `versionCode` against the remote changelog.
 */
sealed interface AppUpdateStatus {

    /** Installed version is up to date (`versionCode >= latestVersion`). */
    data object NoUpdate : AppUpdateStatus

    /**
     * An update is available but not required
     * (`versionCode < latestVersion` and `versionCode >= minimumVersion`).
     *
     * @property changelog Remote changelog used for the decision.
     */
    data class OptionalUpdate(
        val changelog: Changelog,
    ) : AppUpdateStatus

    /**
     * An update is required to continue
     * (`versionCode < minimumVersion`).
     *
     * @property changelog Remote changelog used for the decision.
     */
    data class MandatoryUpdate(
        val changelog: Changelog,
    ) : AppUpdateStatus
}
