package ir.hrka.download.manager.filing

import android.content.Context
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.FileCreationMode
import java.io.File

/**
 * [FileProvider] implementation that resolves download output paths inside app-internal storage.
 *
 * Files are stored under `Context.filesDir`, optionally nested inside one or more subdirectories.
 * This class is selected when [DownloadStorageLocation.Internal] is configured for a download job.
 *
 * ## Use in the module
 * Used by the download worker layer to obtain the local [File] where bytes are written.
 * Partial downloads: when [FileCreationMode.Append] is passed to [provide] and the file already
 * exists, the downloader resumes from the existing byte length.
 *
 * ## How to use
 * ```
 * val fileProvider = InternalFileProvider(
 *     context = applicationContext,
 *     fileName = "model.bin",
 *     fileDirectories = listOf("downloads", "models"),
 *     creationMode = FileCreationMode.Append,
 * )
 * val outputFile = fileProvider.provide()
 * // → /data/data/<package>/files/downloads/models/model.bin
 * ```
 *
 * @property context Android [Context] used to access [Context.filesDir].
 * @property fileName Base name of the output file.
 * @property fileDirectories Nested subdirectory segments under `filesDir`.
 * @property creationMode Strategy applied when the resolved output file already exists.
 *
 * @see FileProvider
 * @see FileCreationMode
 * @see DownloadStorageLocation.Internal
 */
internal class InternalFileProvider(
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
     */
    override fun provide(): File = provideOutputFile(fileName, fileDirectories, creationMode)

    /**
     * Builds the target output [File] and handles collisions with an existing file.
     *
     * Resolution steps:
     * 1. Build the path via [createFileInInternalStorage].
     * 2. If the file does not exist, return it immediately.
     * 3. If the file exists, apply [creationMode]:
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
        var outputFile = createFileInInternalStorage(
            context = context,
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

                FileCreationMode.CreateNew -> createFileInInternalStorage(
                    context = context,
                    directories = fileDirectories,
                    fileName = getUniqueFileName(fileName),
                )
            }
        }

        return outputFile
    }

    /**
     * Constructs a [File] under `context.filesDir` with the given directory chain and [fileName].
     *
     * Missing parent directories are created automatically with [File.mkdirs].
     *
     * Example path:
     * ```
     * filesDir = /data/data/<package>/files
     * directories = ["downloads", "models"]
     * fileName = "weights.bin"
     * result = /data/data/<package>/files/downloads/models/weights.bin
     * ```
     *
     * @param context Android [Context] for accessing internal storage.
     * @param directories Nested folder names relative to `filesDir`; may be empty.
     * @param fileName Output file name including extension.
     * @return A [File] at the computed location (not guaranteed to exist yet).
     */
    private fun createFileInInternalStorage(
        context: Context,
        directories: List<String>,
        fileName: String,
    ): File {
        val outputDir = File(
            context.filesDir,
            directories.joinToString(separator = File.separator),
        )

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        return File(outputDir, fileName)
    }
}
