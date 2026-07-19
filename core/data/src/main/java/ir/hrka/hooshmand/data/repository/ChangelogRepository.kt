package ir.hrka.hooshmand.data.repository

import ir.hrka.hooshmand.model.Changelog

/**
 * Repository for the remote app changelog used by update checks.
 */
interface ChangelogRepository {

    /**
     * Fetches and decodes the remote changelog.
     *
     * @return Domain [Changelog] parsed from GitHub `changelog.json`.
     * @throws java.io.IOException when the network call fails or content cannot be decoded.
     * @throws kotlinx.serialization.SerializationException when JSON parsing fails.
     */
    suspend fun getChangelog(): Changelog
}
