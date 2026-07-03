package ir.hrka.download.manager.filing

import android.content.Context
import android.os.Environment
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.FileCreationMode
import java.io.File

/**
 * [FileProvider] implementation that resolves download output paths inside app-specific external storage.
 *
 * Files are stored under `Context.getExternalFilesDir`, optionally nested inside one or more subdirectories.
 * This class is selected when [DownloadStorageLocation.External] is configured for a download job.
 *
 * ## Use in the module
 * Used by the download worker layer to obtain the local [File] where bytes are written.
 * Partial downloads: when [FileCreationMode.Append] is passed to [provide] and the file already
 * exists, the downloader resumes from the existing byte length.
 *
 * ## How to use
 * ```
 * val fileProvider = ExternalFileProvider(
 *     context = applicationContext,
 *     fileName = "model.bin",
 *     fileDirectories = listOf("downloads", "models"),
 *     creationMode = FileCreationMode.Append,
 * )
 * val outputFile = fileProvider.provide()
 * // → /storage/emulated/0/Android/data/<package>/files/downloads/models/model.bin
 * ```
 *
 * @property context Android [Context] used to access app-specific external storage.
 * @property fileName Base name of the output file.
 * @property fileDirectories Nested subdirectory segments under the external files directory.
 * @property creationMode Strategy applied when the resolved output file already exists.
 *
 * @see FileProvider
 * @see FileCreationMode
 * @see DownloadStorageLocation.External
 */
internal class ExternalFileProvider(
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
     * @throws IllegalStateException if app-specific external storage is unavailable or not mounted.
     */
    override fun provide(): File = provideOutputFile(fileName, fileDirectories, creationMode)

    /**
     * Builds the target output [File] and handles collisions with an existing file.
     *
     * Resolution steps:
     * 1. Verify external storage access via [requireExternalStorageAccess].
     * 2. Build the path via [createFileInExternalStorage].
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
        val baseDir = requireExternalStorageAccess()

        var outputFile = createFileInExternalStorage(
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

                FileCreationMode.CreateNew -> createFileInExternalStorage(
                    baseDir = baseDir,
                    directories = fileDirectories,
                    fileName = getUniqueFileName(fileName),
                )
            }
        }

        return outputFile
    }

    /**
     * Verifies that app-specific external storage is available.
     *
     * Does not request permissions.
     */
    private fun requireExternalStorageAccess(): File {
        val baseDir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException(
                "App-specific external storage directory is not available",
            )

        val storageState = Environment.getExternalStorageState(baseDir)
        if (storageState != Environment.MEDIA_MOUNTED) {
            throw IllegalStateException(
                "External storage is not writable. Current state: $storageState",
            )
        }

        return baseDir
    }

    /**
     * Constructs a [File] under [baseDir] with the given directory chain and [fileName].
     *
     * Missing parent directories are created automatically with [File.mkdirs].
     *
     * @param baseDir Root directory from [requireExternalStorageAccess].
     * @param directories Nested folder names relative to [baseDir]; may be empty.
     * @param fileName Output file name including extension.
     * @return A [File] at the computed location (not guaranteed to exist yet).
     */
    private fun createFileInExternalStorage(
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
