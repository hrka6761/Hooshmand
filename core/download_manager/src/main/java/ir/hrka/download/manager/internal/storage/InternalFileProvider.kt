package ir.hrka.download.manager.internal.storage

import android.content.Context
import ir.hrka.download.manager.error.StorageDownloadException
import ir.hrka.download.manager.model.FileCreationMode
import java.io.File

/**
 * Writes downloads under [Context.getFilesDir].
 */
internal class InternalFileProvider(
    private val context: Context,
    private val fileName: String,
    private val fileDirectories: List<String>,
    private val creationMode: FileCreationMode,
) : FileProvider {

    override fun provide(): File {
        val outputFile = createFile(fileName)
        return OutputFileResolver.resolveExisting(
            resolvedFile = outputFile,
            creationMode = creationMode,
            createUnique = { name -> createFile(getUniqueFileName(name)) },
        )
    }

    private fun createFile(name: String): File {
        val outputDir =
            File(
                context.filesDir,
                fileDirectories.joinToString(separator = File.separator),
            )
        if (!outputDir.exists() && !outputDir.mkdirs() && !outputDir.exists()) {
            throw StorageDownloadException(
                "Could not create internal storage directory: ${outputDir.absolutePath}",
            )
        }
        return File(outputDir, name)
    }
}
