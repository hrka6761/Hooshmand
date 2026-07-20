package ir.hrka.download.manager.internal.storage

import android.content.Context
import android.os.Environment
import ir.hrka.download.manager.error.StorageDownloadException
import ir.hrka.download.manager.model.FileCreationMode
import java.io.File

/**
 * Writes downloads under [Context.getExternalFilesDir].
 */
internal class ExternalFileProvider(
    private val context: Context,
    private val fileName: String,
    private val fileDirectories: List<String>,
    private val creationMode: FileCreationMode,
) : FileProvider {

    override fun provide(): File {
        val baseDir = requireExternalStorageAccess()
        val outputFile = createFile(baseDir, fileName)
        return OutputFileResolver.resolveExisting(
            resolvedFile = outputFile,
            creationMode = creationMode,
            createUnique = { name -> createFile(baseDir, getUniqueFileName(name)) },
        )
    }

    private fun requireExternalStorageAccess(): File {
        val baseDir =
            context.getExternalFilesDir(null)
                ?: throw StorageDownloadException(
                    "App-specific external storage directory is not available.",
                )

        val storageState = Environment.getExternalStorageState(baseDir)
        if (storageState != Environment.MEDIA_MOUNTED) {
            throw StorageDownloadException(
                "External storage is not writable. Current state: $storageState",
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
                "Could not create external storage directory: ${outputDir.absolutePath}",
            )
        }
        return File(outputDir, name)
    }
}
