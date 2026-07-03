package ir.hrka.download.manager.filing

import android.content.Context
import android.os.Environment
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.FileCreationMode
import java.io.File

/**
 * [FileProvider] implementation that resolves download output paths inside shared public storage.
 *
 * Files are stored under the public Downloads directory (`Environment.DIRECTORY_DOWNLOADS`),
 * optionally nested inside one or more subdirectories. This class is selected when
 * [DownloadStorageLocation.Public] is configured for a download job.
 *
 * ## Use in the module
 * Used by the download worker layer to obtain the local [File] where bytes are written.
 * Partial downloads: when [FileCreationMode.Append] is passed to [provide] and the file already
 * exists, the downloader resumes from the existing byte length.
 *
 * ## How to use
 * ```
 * val fileProvider = PublicFileProvider(
 *     context = applicationContext,
 *     fileName = "model.bin",
 *     fileDirectories = listOf("downloads", "models"),
 *     creationMode = FileCreationMode.Append,
 * )
 * val outputFile = fileProvider.provide()
 * // → /storage/emulated/0/Download/downloads/models/model.bin
 * ```
 *
 * @property context Android [Context] used to resolve public storage paths.
 * Storage permissions must be granted by the host app before downloading; see
 * [ir.hrka.download.manager.api.DownloadManagerHostRequirements].
 * @property fileName Base name of the output file.
 * @property fileDirectories Nested subdirectory segments under the public Downloads directory.
 * @property creationMode Strategy applied when the resolved output file already exists.
 *
 * @see FileProvider
 * @see FileCreationMode
 * @see DownloadStorageLocation.Public
 */
internal class PublicFileProvider(
    private val context: Context,
    private val fileName: String,
    private val fileDirectories: List<String>,
    private val creationMode: FileCreationMode,
) : FileProvider {

    /**
     * Provides the local [File] where the download should be written.
     *
     * Delegates to [provideOutputFile] using the constructor-supplied [fileName],
     * [fileDirectories], and [creationMode].
     *
     * @return A [File] pointing to the resolved output location.
     * @throws IllegalStateException if public storage is unavailable or not writable.
     * @throws SecurityException if public storage access was not granted by the host app.
     */
    override fun provide(): File = provideOutputFile(fileName, fileDirectories, creationMode)

    /**
     * Builds the target output [File] and handles collisions with an existing file.
     *
     * Resolution steps:
     * 1. Verify public storage access via [requirePublicStorageAccess].
     * 2. Build the path via [createFileInPublicStorage].
     * 3. If the file does not exist, return it immediately.
     * 4. If the file exists, apply [creationMode]:
     *    - [FileCreationMode.Overwrite] — delete the existing file and reuse the same path.
     *    - [FileCreationMode.Append] — return the existing file unchanged (resume-friendly).
     *    - [FileCreationMode.CreateNew] — generate a unique name via [getUniqueFileName] and
     *      resolve a new path.
     *
     * @return The [File] the downloader should open for writing.
     */
    private fun provideOutputFile(
        fileName: String,
        fileDirectories: List<String>,
        creationMode: FileCreationMode,
    ): File {
        val baseDir = requirePublicStorageAccess()

        var outputFile = createFileInPublicStorage(
            baseDir = baseDir,
            directories = fileDirectories,
            fileName = fileName,
        )

        if (outputFile.exists()) {
            outputFile = when (creationMode) {
                FileCreationMode.Overwrite -> {
                    outputFile.delete()
                    outputFile
                }

                FileCreationMode.Append -> outputFile

                FileCreationMode.CreateNew -> createFileInPublicStorage(
                    baseDir = baseDir,
                    directories = fileDirectories,
                    fileName = getUniqueFileName(fileName),
                )
            }
        }

        return outputFile
    }

    /**
     * Verifies that public storage is available.
     *
     * Does not request permissions. The host app must obtain storage access before
     * using [DownloadStorageLocation.Public].
     *
     * @return Writable public Downloads root directory.
     * @throws IllegalStateException if public storage is unavailable or not mounted.
     * @throws SecurityException if required storage permissions were not granted by the host app.
     */
    private fun requirePublicStorageAccess(): File {
        requirePublicStoragePermission()

        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val storageState = Environment.getExternalStorageState(baseDir)
        if (storageState != Environment.MEDIA_MOUNTED) {
            throw IllegalStateException(
                "Public storage is not writable. Current state: $storageState",
            )
        }

        return baseDir
    }

    /**
     * Fails fast when [Environment.isExternalStorageManager] is false.
     * The host app must grant access before using public storage.
     */
    private fun requirePublicStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            throw SecurityException(
                "MANAGE_EXTERNAL_STORAGE permission is required for public storage access but was not granted",
            )
        }
    }

    /**
     * Constructs a [File] under the public Downloads [baseDir] with the given directory chain
     * and [fileName].
     *
     * Missing parent directories are created automatically with [File.mkdirs].
     *
     * @param baseDir Root directory from [requirePublicStorageAccess].
     * @param directories Nested folder names relative to [baseDir]; may be empty.
     * @param fileName Output file name including extension.
     * @return A [File] at the computed location (not guaranteed to exist yet).
     */
    private fun createFileInPublicStorage(
        baseDir: File,
        directories: List<String>,
        fileName: String,
    ): File {
        val outputDir = File(
            baseDir,
            directories.joinToString(separator = File.separator),
        )

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        return File(outputDir, fileName)
    }
}
