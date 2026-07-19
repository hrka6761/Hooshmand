package ir.hrka.hooshmand.domain

import ir.hrka.hooshmand.data.repository.ChangelogRepository
import javax.inject.Inject

/**
 * Checks whether the installed app must or may update, using the remote changelog.
 *
 * Comparison rules:
 * - `appVersionCode >= latestVersion` → [AppUpdateStatus.NoUpdate]
 * - `appVersionCode < minimumVersion` → [AppUpdateStatus.MandatoryUpdate]
 * - otherwise (`appVersionCode < latestVersion`) → [AppUpdateStatus.OptionalUpdate]
 */
class CheckAppUpdateUseCase @Inject constructor(
    private val changelogRepository: ChangelogRepository,
) {

    /**
     * Loads the remote changelog and evaluates [appVersionCode] against it.
     *
     * @param appVersionCode Installed Android `versionCode`.
     * @return The resulting [AppUpdateStatus].
     * @throws java.io.IOException when the changelog cannot be fetched or decoded.
     * @throws kotlinx.serialization.SerializationException when changelog JSON is invalid.
     */
    suspend operator fun invoke(appVersionCode: Int): AppUpdateStatus {
        val changelog = changelogRepository.getChangelog()
        return when {
            appVersionCode >= changelog.latestVersion -> AppUpdateStatus.NoUpdate
            appVersionCode < changelog.minimumVersion -> AppUpdateStatus.MandatoryUpdate(changelog)
            else -> AppUpdateStatus.OptionalUpdate(changelog)
        }
    }
}
