package ir.hrka.hooshmand.ai_chat.impl.download

import ir.hrka.download.manager.MultipartItemDownloadData
import ir.hrka.download.manager.SingleItemDownloadData
import ir.hrka.hooshmand.model.ModelEntry

/**
 * Maps a remote [ModelEntry] to [DownloadManager] multipart configuration.
 *
 * @throws IllegalStateException when [ModelEntry.multiPartAddresses] is empty.
 */
internal fun ModelEntry.toMultipartDownloadData(): MultipartItemDownloadData {
    require(multiPartAddresses.isNotEmpty()) {
        "Model '$modelName' has no multi_part_address entries"
    }
    return MultipartItemDownloadData(
        itemParts =
            multiPartAddresses.map { url ->
                SingleItemDownloadData(
                    url = url,
                    fileName = modelName,
                    fileSize = null,
                )
            },
        totalFileSize = null,
    )
}

/**
 * Maps a remote [ModelEntry] to a single-file download configuration.
 *
 * @throws IllegalStateException when [ModelEntry.singlePartAddress] is null/blank.
 */
internal fun ModelEntry.toSingleDownloadData(): SingleItemDownloadData {
    val url = singlePartAddress?.takeIf { it.isNotBlank() }
        ?: error("Model '$modelName' has no single_part_address")
    return SingleItemDownloadData(
        url = url,
        fileName = modelName,
        fileSize = null,
    )
}

/**
 * Whether [ModelEntry.singlePartAddress] is present and should be used for download.
 *
 * Takes priority over [ModelEntry.multiPartAddresses].
 */
internal fun ModelEntry.hasSinglePartAddress(): Boolean =
    !singlePartAddress.isNullOrBlank()

/**
 * Whether this entry should be downloaded as multiple appended parts.
 *
 * Used only when [hasSinglePartAddress] is `false`.
 */
internal fun ModelEntry.usesMultipartDownload(): Boolean =
    !hasSinglePartAddress() && multiPartAddresses.isNotEmpty()
