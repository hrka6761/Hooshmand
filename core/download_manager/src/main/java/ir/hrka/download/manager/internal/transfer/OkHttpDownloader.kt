package ir.hrka.download.manager.internal.transfer

import ir.hrka.download.manager.error.DownloadError
import ir.hrka.download.manager.error.EmptyResponseException
import ir.hrka.download.manager.error.HttpDownloadException
import ir.hrka.download.manager.error.ResumeNotSupportedException
import ir.hrka.download.manager.internal.storage.FileProvider
import ir.hrka.download.manager.internal.work.ActiveDownloadTransferRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * OkHttp-based download engine that streams HTTP response bytes to disk.
 *
 * Supports resumable downloads via [sourceOffset] and append writes via [appendToOutput].
 * Progress is reported to [OkHttpListener] at roughly 200 ms intervals.
 *
 * @property fileProvider Supplies the output [File] when [outputFile] is not provided.
 */
internal class OkHttpDownloader(
    private val fileProvider: FileProvider,
) {

    private var httpInputStream: InputStream? = null
    private var fileOutputStream: FileOutputStream? = null

    /**
     * Downloads [fileUrl] into [outputFile] or the file from [fileProvider].
     *
     * @param listener Progress and completion callbacks.
     * @param fileUrl Remote URL to fetch.
     * @param expectedSegmentBytes Expected bytes for the current segment (part or whole file).
     * @param headers Optional HTTP headers.
     * @param outputFile Target file; resolved via [fileProvider] when `null`.
     * @param appendToOutput When `true`, opens the output stream in append mode.
     * @param sourceOffset Bytes already received from [fileUrl]; sent as a HTTP Range header when positive.
     * @param overallReceivedBytes Bytes already on disk before this segment starts (for overall progress).
     * @param overallTotalBytes Total expected bytes for the full output file, if known.
     * @param downloadId Work id used to cancel the in-flight OkHttp call on pause/stop.
     * @param shouldStop Invoked during transfer; return `true` to stop early (pause/cancel).
     */
    suspend fun startDownload(
        listener: OkHttpListener,
        fileUrl: String,
        expectedSegmentBytes: Long,
        headers: Map<String, String> = emptyMap(),
        outputFile: File? = null,
        appendToOutput: Boolean = false,
        sourceOffset: Long = 0L,
        overallReceivedBytes: Long = 0L,
        overallTotalBytes: Long? = null,
        downloadId: String? = null,
        shouldStop: () -> Boolean = { false },
    ) {
        val targetFile = outputFile ?: fileProvider.provide()
        if (shouldStop()) {
            listener.onDownloadFailed(
                targetFile,
                IOException(DownloadError.STOPPED_MESSAGE),
            )
            return
        }

        var segmentReceivedBytes = sourceOffset
        var deltaBytes = 0L
        var lastSetProgressTs = 0L
        val bytesReadSizeBuffer = mutableListOf<Long>()
        val bytesReadLatencyBuffer = mutableListOf<Long>()

        val client = DownloadHttpClient.instance
        val request = buildRequest(sourceOffset, fileUrl, headers)
        val call = client.newCall(request)
        downloadId?.let { ActiveDownloadTransferRegistry.register(it, call) }

        try {
            call.execute().use { response ->
                if (shouldStop()) {
                    listener.onDownloadFailed(
                        targetFile,
                        IOException(DownloadError.STOPPED_MESSAGE),
                    )
                    return
                }

                if (!response.isSuccessful) {
                    listener.onDownloadFailed(
                        targetFile,
                        HttpDownloadException(
                            httpCode = response.code,
                            message = "HTTP ${response.code}: ${response.message}",
                        ),
                    )
                    return
                }

                if (sourceOffset > 0L && response.code != HTTP_PARTIAL_CONTENT) {
                    listener.onDownloadFailed(
                        targetFile,
                        ResumeNotSupportedException(
                            message =
                                "Expected HTTP 206 for resume at offset $sourceOffset, " +
                                    "but server returned ${response.code}.",
                        ),
                    )
                    return
                }

                val body = response.body
                if (body == null) {
                    listener.onDownloadFailed(targetFile, EmptyResponseException())
                    return
                }

                val resolvedSegmentTotal =
                    HttpResponseSizeParser.resolveTotalBytes(response, sourceOffset)
                        ?: expectedSegmentBytes.takeIf { it > 0L }
                val effectiveOverallTotal = overallTotalBytes ?: resolvedSegmentTotal

                listener.onProgressUpdate(
                    file = targetFile,
                    downloadedBytes = overallReceivedBytes + sourceOffset,
                    downloadRate = 0f,
                    remainingTime = 0f,
                    totalBytes = effectiveOverallTotal,
                    segmentTotalBytes = resolvedSegmentTotal,
                )

                FileOutputStream(targetFile, appendToOutput).use { output ->
                    fileOutputStream = output
                    body.byteStream().use { input ->
                        httpInputStream = input
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            segmentReceivedBytes += bytesRead
                            deltaBytes += bytesRead

                            if (shouldStop()) {
                                output.flush()
                                publishProgress(
                                    listener = listener,
                                    targetFile = targetFile,
                                    overallReceivedBytes = overallReceivedBytes,
                                    segmentReceivedBytes = segmentReceivedBytes,
                                    effectiveOverallTotal = effectiveOverallTotal,
                                    resolvedSegmentTotal = resolvedSegmentTotal,
                                    downloadRate = 0f,
                                )
                                listener.onDownloadFailed(
                                    targetFile,
                                    IOException(DownloadError.STOPPED_MESSAGE),
                                )
                                return
                            }

                            val curTs = System.currentTimeMillis()
                            if (curTs - lastSetProgressTs > PROGRESS_INTERVAL_MS) {
                                val bytesPerMs =
                                    calculateBytesPerMs(
                                        deltaBytes = deltaBytes,
                                        lastSetProgressTs = lastSetProgressTs,
                                        curTs = curTs,
                                        bytesReadSizeBuffer = bytesReadSizeBuffer,
                                        bytesReadLatencyBuffer = bytesReadLatencyBuffer,
                                    ).also { deltaBytes = 0L }

                                publishProgress(
                                    listener = listener,
                                    targetFile = targetFile,
                                    overallReceivedBytes = overallReceivedBytes,
                                    segmentReceivedBytes = segmentReceivedBytes,
                                    effectiveOverallTotal = effectiveOverallTotal,
                                    resolvedSegmentTotal = resolvedSegmentTotal,
                                    downloadRate = bytesPerMs * 1000f,
                                    bytesPerMs = bytesPerMs,
                                )

                                lastSetProgressTs = curTs
                            }
                        }

                        listener.onDownloadCompleted(targetFile)
                    }
                }
            }
        } catch (e: IOException) {
            if (call.isCanceled() || shouldStop()) {
                listener.onDownloadFailed(
                    targetFile,
                    IOException(DownloadError.STOPPED_MESSAGE),
                )
            } else {
                listener.onDownloadFailed(targetFile, e)
            }
        } finally {
            downloadId?.let { ActiveDownloadTransferRegistry.unregister(it) }
            stopDownload()
        }
    }

    /**
     * Closes open HTTP and file streams.
     *
     * Safe to call when no download is active or after completion/failure.
     */
    suspend fun stopDownload() {
        withContext(Dispatchers.IO) {
            runCatching { fileOutputStream?.flush() }
            runCatching { fileOutputStream?.close() }
            runCatching { httpInputStream?.close() }
            fileOutputStream = null
            httpInputStream = null
        }
    }

    private suspend fun publishProgress(
        listener: OkHttpListener,
        targetFile: File,
        overallReceivedBytes: Long,
        segmentReceivedBytes: Long,
        effectiveOverallTotal: Long?,
        resolvedSegmentTotal: Long?,
        downloadRate: Float,
        bytesPerMs: Float = 0f,
    ) {
        val overallReceived = overallReceivedBytes + segmentReceivedBytes
        val remainingMs =
            calculateRemainingMs(
                bytesPerMs = if (bytesPerMs > 0f) bytesPerMs else 0f,
                totalBytes = effectiveOverallTotal,
                downloadedBytes = overallReceived,
            )

        listener.onProgressUpdate(
            file = targetFile,
            downloadedBytes = overallReceived,
            downloadRate = downloadRate,
            remainingTime = remainingMs,
            totalBytes = effectiveOverallTotal,
            segmentTotalBytes = resolvedSegmentTotal,
        )
    }

    private fun calculateBytesPerMs(
        deltaBytes: Long,
        lastSetProgressTs: Long,
        curTs: Long,
        bytesReadSizeBuffer: MutableList<Long>,
        bytesReadLatencyBuffer: MutableList<Long>,
    ): Float {
        if (lastSetProgressTs == 0L) return 0f

        if (bytesReadSizeBuffer.size == RATE_SAMPLE_COUNT) bytesReadSizeBuffer.removeAt(0)
        bytesReadSizeBuffer.add(deltaBytes)
        if (bytesReadLatencyBuffer.size == RATE_SAMPLE_COUNT) bytesReadLatencyBuffer.removeAt(0)
        bytesReadLatencyBuffer.add(curTs - lastSetProgressTs)

        return bytesReadSizeBuffer.sum().toFloat() / bytesReadLatencyBuffer.sum()
    }

    private fun calculateRemainingMs(
        bytesPerMs: Float,
        totalBytes: Long?,
        downloadedBytes: Long,
    ): Float {
        if (bytesPerMs <= 0f || totalBytes == null || totalBytes <= 0L) return 0f
        return (totalBytes - downloadedBytes) / bytesPerMs
    }

    private fun buildRequest(
        sourceOffset: Long,
        url: String,
        headers: Map<String, String>,
    ): Request {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        if (sourceOffset > 0L) {
            requestBuilder.addHeader("Range", "bytes=$sourceOffset-")
        }

        return requestBuilder.build()
    }

    private companion object {
        const val HTTP_PARTIAL_CONTENT: Int = 206
        const val PROGRESS_INTERVAL_MS: Long = 200L
        const val RATE_SAMPLE_COUNT: Int = 5
        const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024
    }
}
