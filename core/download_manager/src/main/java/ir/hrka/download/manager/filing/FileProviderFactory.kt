package ir.hrka.download.manager.filing

import android.content.Context
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.FileCreationMode
import ir.hrka.download.manager.internal.work.DownloadWorkInput

/**
 * Factory for storage-specific [FileProvider] implementations.
 *
 * Selects the correct provider based on [DownloadWorkInput.storageLocation] and passes
 * output path configuration from the work input.
 *
 * @see InternalFileProvider
 * @see ExternalFileProvider
 * @see PublicFileProvider
 */
internal object FileProviderFactory {

    /**
     * Creates a [FileProvider] configured for [input].
     *
     * @param context Application [Context] for storage access.
     * @param input Work input containing [DownloadWorkInput.fileName], [DownloadWorkInput.directories],
     * [DownloadWorkInput.storageLocation], and [DownloadWorkInput.creationMode].
     * @return A provider that resolves the on-disk output file for the download job.
     */
    fun create(context: Context, input: DownloadWorkInput): FileProvider =
        when (input.storageLocation) {
            DownloadStorageLocation.Internal -> InternalFileProvider(
                context = context,
                fileName = input.fileName,
                fileDirectories = input.directories,
                creationMode = input.creationMode,
            )

            DownloadStorageLocation.External -> ExternalFileProvider(
                context = context,
                fileName = input.fileName,
                fileDirectories = input.directories,
                creationMode = input.creationMode,
            )

            DownloadStorageLocation.Public -> PublicFileProvider(
                context = context,
                fileName = input.fileName,
                fileDirectories = input.directories,
                creationMode = input.creationMode,
            )
        }
}
