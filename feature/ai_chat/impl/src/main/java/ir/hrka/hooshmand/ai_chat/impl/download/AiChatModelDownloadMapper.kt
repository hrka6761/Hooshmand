package ir.hrka.hooshmand.ai_chat.impl.download

import ir.hrka.download.manager.MultipartItemDownloadData
import ir.hrka.download.manager.SingleItemDownloadData
import ir.hrka.hooshmand.ai_chat.impl.AiChatModelDownloadParts

/** Maps [AiChatModelDownloadParts] to [DownloadManager] multipart configuration. */
internal fun AiChatModelDownloadParts.toMultipartDownloadData(): MultipartItemDownloadData =
    MultipartItemDownloadData(
        itemParts =
            parts.map { part ->
                SingleItemDownloadData(
                    url = part.url,
                    fileName = MODEL_FILE_NAME,
                    fileSize = part.sizeInBytes,
                )
            },
        totalFileSize = totalSizeInBytes,
    )
