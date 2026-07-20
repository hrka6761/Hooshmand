package ir.hrka.download.manager.internal.storage

import android.content.Context
import ir.hrka.download.manager.internal.work.DownloadWorkInput
import ir.hrka.download.manager.model.DownloadStorageLocation
import ir.hrka.download.manager.model.FileCreationMode

/**
 * Factory for storage-specific [FileProvider] implementations.
 */
internal object FileProviderFactory {

    /**
     * Creates a [FileProvider] configured for [input] and [creationMode].
     *
     * @param creationMode Effective collision mode; may differ from [DownloadWorkInput.creationMode]
     * when the worker is retrying and must preserve partial bytes.
     */
    fun create(
        context: Context,
        input: DownloadWorkInput,
        creationMode: FileCreationMode = input.creationMode,
    ): FileProvider =
        when (input.storageLocation) {
            DownloadStorageLocation.Internal ->
                InternalFileProvider(
                    context = context,
                    fileName = input.fileName,
                    fileDirectories = input.directories,
                    creationMode = creationMode,
                )

            DownloadStorageLocation.External ->
                ExternalFileProvider(
                    context = context,
                    fileName = input.fileName,
                    fileDirectories = input.directories,
                    creationMode = creationMode,
                )

            DownloadStorageLocation.Public ->
                PublicFileProvider(
                    context = context,
                    fileName = input.fileName,
                    fileDirectories = input.directories,
                    creationMode = creationMode,
                )
        }
}
