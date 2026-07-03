package ir.hrka.download.manager.filing

import ir.hrka.download.manager.FileCreationMode
import java.io.File

/**
 * Resolves the local output [File] for a download job.
 *
 * Implementations map a [ir.hrka.download.manager.DownloadStorageLocation] to a concrete
 * filesystem path and apply [FileCreationMode] when a file with the same name already exists.
 *
 * @see InternalFileProvider
 * @see ExternalFileProvider
 * @see PublicFileProvider
 */
internal interface FileProvider {

    /**
     * Returns the [File] where downloaded bytes should be written.
     *
     * The returned file may already exist when [FileCreationMode.Append] is configured,
     * allowing the downloader to resume from the existing length.
     *
     * @return Resolved output file, with parent directories created when necessary.
     */
    fun provide(): File

    /**
     * Produces a collision-free file name by inserting a millisecond timestamp before the extension.
     *
     * Examples:
     * - `video.mp4` → `video_1712345678901.mp4`
     * - `README` → `README_1712345678901`
     *
     * @param fileName Original file name.
     * @return A new file name safe to use with [FileCreationMode.CreateNew].
     */
    fun getUniqueFileName(fileName: String): String {
        val timestamp = System.currentTimeMillis().toString()
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) {
            "${fileName.substring(0, dotIndex)}_$timestamp${fileName.substring(dotIndex)}"
        } else {
            "${fileName}_$timestamp"
        }
    }
}
