package ir.hrka.download.manager.internal.storage

import android.content.Context
import android.os.Environment
import ir.hrka.download.manager.error.PermissionDownloadException
import ir.hrka.download.manager.error.StorageDownloadException
import ir.hrka.download.manager.model.FileCreationMode
import java.io.File

/**
 * Writes downloads under the public Downloads directory.
 *
 * Requires [Environment.isExternalStorageManager] to be granted by the host app.
 */
internal class PublicFileProvider(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
    private val fileName: String,
    private val fileDirectories: List<String>,
    private val creationMode: FileCreationMode,
) : FileProvider {

    override fun provide(): File {
        val baseDir = requirePublicStorageAccess()
        val outputFile = createFile(baseDir, fileName)
        return OutputFileResolver.resolveExisting(
            resolvedFile = outputFile,
            creationMode = creationMode,
            createUnique = { name -> createFile(baseDir, getUniqueFileName(name)) },
        )
    }

    private fun requirePublicStorageAccess(): File {
        if (!Environment.isExternalStorageManager()) {
            throw PermissionDownloadException(
                "MANAGE_EXTERNAL_STORAGE permission is required for public storage access " +
                    "but was not granted.",
            )
        }

        val baseDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val storageState = Environment.getExternalStorageState(baseDir)
        if (storageState != Environment.MEDIA_MOUNTED) {
            throw StorageDownloadException(
                "Public storage is not writable. Current state: $storageState",
            )
        }
        return baseDir
    }

    private fun createFile(baseDir: File, name: String): File {
        val outputDir =
            File(
                baseDir,
                fileDirectories.joinToString(separator = File.separator),
            )
        if (!outputDir.exists() && !outputDir.mkdirs() && !outputDir.exists()) {
            throw StorageDownloadException(
                "Could not create public storage directory: ${outputDir.absolutePath}",
            )
        }
        return File(outputDir, name)
    }
}
