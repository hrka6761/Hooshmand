package ir.hrka.download.manager.internal.storage

import ir.hrka.download.manager.model.FileCreationMode
import java.io.File

/**
 * Shared collision-handling logic for storage [FileProvider] implementations.
 */
internal object OutputFileResolver {

    /**
     * Applies [creationMode] when [resolvedFile] already exists.
     *
     * @param resolvedFile Candidate output path.
     * @param creationMode Collision strategy.
     * @param createUnique Alternative path factory for [FileCreationMode.CreateNew].
     * @return The file the downloader should open for writing.
     */
    fun resolveExisting(
        resolvedFile: File,
        creationMode: FileCreationMode,
        createUnique: (String) -> File,
    ): File {
        if (!resolvedFile.exists()) return resolvedFile

        return when (creationMode) {
            FileCreationMode.Overwrite -> {
                if (!resolvedFile.delete() && resolvedFile.exists()) {
                    throw IllegalStateException(
                        "Could not delete existing file: ${resolvedFile.absolutePath}",
                    )
                }
                resolvedFile
            }

            FileCreationMode.Append -> resolvedFile

            FileCreationMode.CreateNew -> createUnique(resolvedFile.name)
        }
    }
}
