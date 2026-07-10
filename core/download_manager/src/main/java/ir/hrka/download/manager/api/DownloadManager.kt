package ir.hrka.download.manager.api

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.DownloadType
import ir.hrka.download.manager.FileCreationMode
import ir.hrka.download.manager.MultipartItemDownloadData
import ir.hrka.download.manager.SingleItemDownloadData
import ir.hrka.download.manager.internal.work.DownloadWorkInput
import ir.hrka.download.manager.internal.work.DownloadWorker
import ir.hrka.download.manager.internal.work.DownloadWorkerControl
import ir.hrka.download.manager.internal.listener.DownloadListenerBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID

/**
 * Entry point for starting, pausing, resuming, and stopping file downloads.
 *
 * Configure a job through [Builder], then call [startDownload] to enqueue background work
 * via WorkManager. Downloads continue when the app is in the background or the process
 * is killed.
 *
 * ## Host application setup
 * Manifest permissions and [DownloadActionReceiver] are merged from this module automatically.
 * The host app must register [DownloadWorkerFactory] and request runtime permissions via
 * [DownloadManagerPermissions]. See [DownloadManagerHostRequirements].
 *
 * Two download modes are supported and are mutually exclusive:
 * - [DownloadType.SingleFile] — configured with [SingleItemDownloadData] via
 *   [Builder.setSingleItemDownloadData].
 * - [DownloadType.MultiParts] — configured with [MultipartItemDownloadData] via
 *   [Builder.setMultiPartsDownloadData]; parts are appended sequentially into one file.
 *
 * ## Single-file download
 * ```
 * DownloadManager.Builder(context)
 *     .setSingleItemDownloadData(
 *         SingleItemDownloadData(
 *             url = url,
 *             fileName = "video.mp4",
 *             fileSize = 12_000_000L,
 *         ),
 *     )
 *     .setFileLocation(DownloadStorageLocation.Internal)
 *     .setDownloadListener(listener)
 *     .build()
 *     .startDownload()
 * ```
 *
 * ## Multi-part download
 * ```
 * DownloadManager.Builder(context)
 *     .setMultiPartsDownloadData(
 *         MultipartItemDownloadData(
 *             itemParts = listOf(
 *                 SingleItemDownloadData(url = part1Url, fileName = "video.mp4", fileSize = 5_000_000L),
 *                 SingleItemDownloadData(url = part2Url, fileName = "video.mp4", fileSize = 7_000_000L),
 *             ),
 *         ),
 *     )
 *     .setDownloadListener(listener)
 *     .build()
 *     .startDownload()
 * ```
 *
 * @see DownloadManagerHostRequirements
 * @see Builder
 * @see DownloadListener
 * @see SingleItemDownloadData
 * @see MultipartItemDownloadData
 * @see DownloadType
 */
class DownloadManager private constructor(
    context: Context,
    private val downloadId: String,
    private val singleItemDownloadData: SingleItemDownloadData?,
    private val multipartItemDownloadData: MultipartItemDownloadData?,
    private val fileDirectories: List<String>,
    private val downloadType: DownloadType,
    private val fileLocation: DownloadStorageLocation,
    private val creationMode: FileCreationMode,
    private val downloadListener: DownloadListener?,
) {

    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listenerBridge: DownloadListenerBridge? = downloadListener?.let {
        DownloadListenerBridge(workManager, it, listenerScope)
    }

    private var workId: UUID? = null

    /**
     * Enqueues and starts the configured download as a WorkManager foreground job.
     *
     * Uses [ExistingWorkPolicy.KEEP] so repeated calls do not enqueue duplicate workers
     * for the same [downloadId].
     */
    fun startDownload() {
        val workInput = DownloadWorkInput.fromDownloadConfig(
            downloadId = downloadId,
            downloadType = downloadType,
            singleItemDownloadData = singleItemDownloadData,
            multipartItemDownloadData = multipartItemDownloadData,
            directories = fileDirectories,
            storageLocation = fileLocation,
            creationMode = creationMode,
        )

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workInput.toWorkData())
            .addTag(DownloadWorkInput.workTag(downloadId))
            .build()

        workId = request.id
        listenerBridge?.reset()
        DownloadWorkerControl.resetForNewDownload(downloadId)

        workManager.enqueueUniqueWork(
            downloadId,
            ExistingWorkPolicy.KEEP,
            request,
        )

        workId?.let { id -> listenerBridge?.observe(id) }
    }

    /**
     * Cancels the active download and releases in-flight network and file resources.
     *
     * Partially written data may remain on disk depending on [FileCreationMode].
     */
    fun stopDownload() {
        DownloadWorkerControl.requestCancel(downloadId)
        workManager.cancelUniqueWork(downloadId)
        listenerBridge?.cancel()
    }

    /**
     * Pauses an in-progress download while keeping partial data on disk for later resume.
     */
    fun pauseDownload() {
        DownloadWorkerControl.requestPause(downloadId)
    }

    /**
     * Resumes a previously paused download from the last persisted byte offset.
     */
    fun resumeDownload() {
        DownloadWorkerControl.requestResume(downloadId)
    }

    /**
     * Fluent builder for [DownloadManager].
     *
     * Supply exactly one of [setSingleItemDownloadData] or [setMultiPartsDownloadData].
     * Calling either setter also sets the corresponding [DownloadType].
     *
     * @param context Application or activity [Context]. Prefer `applicationContext` to avoid leaks.
     */
    class Builder(
        private val context: Context,
    ) {

        private var singleItemDownloadData: SingleItemDownloadData? = null
        private var multipartItemDownloadData: MultipartItemDownloadData? = null
        private var directories: List<String> = emptyList()
        private var downloadType: DownloadType = DownloadType.SingleFile
        private var fileLocation: DownloadStorageLocation = DownloadStorageLocation.Public
        private var creationMode: FileCreationMode = FileCreationMode.Overwrite
        private var downloadListener: DownloadListener? = null

        /**
         * Configures a [DownloadType.SingleFile] job.
         *
         * Sets [downloadType] to [DownloadType.SingleFile]. Must not be combined with
         * [setMultiPartsDownloadData].
         *
         * @param singleItemDownloadData URL, output file name, size, headers, and checksum.
         * @return This builder for chaining.
         */
        fun setSingleItemDownloadData(singleItemDownloadData: SingleItemDownloadData): Builder {
            this.singleItemDownloadData = singleItemDownloadData
            this.downloadType = DownloadType.SingleFile
            return this
        }

        /**
         * Configures a [DownloadType.MultiParts] job.
         *
         * Parts are downloaded in order and appended into one output file.
         * Sets [downloadType] to [DownloadType.MultiParts]. Must not be combined with
         * [setSingleItemDownloadData].
         *
         * @param multipartItemDownloadData Ordered part list and per-part metadata.
         * @return This builder for chaining.
         */
        fun setMultiPartsDownloadData(multipartItemDownloadData: MultipartItemDownloadData): Builder {
            this.multipartItemDownloadData = multipartItemDownloadData
            this.downloadType = DownloadType.MultiParts
            return this
        }

        /**
         * Sets nested subdirectory segments under the chosen [DownloadStorageLocation].
         *
         * @param directories Ordered folder names (for example `listOf("downloads", "v1")`).
         * @return This builder for chaining.
         */
        fun setDirectories(directories: List<String>): Builder {
            this.directories = directories
            return this
        }

        /**
         * Sets the on-device storage area where the output file is written.
         *
         * @param fileLocation Target storage; defaults to [DownloadStorageLocation.Public].
         * @return This builder for chaining.
         */
        fun setFileLocation(fileLocation: DownloadStorageLocation): Builder {
            this.fileLocation = fileLocation
            return this
        }

        /**
         * Sets how name collisions with an existing file are handled.
         *
         * @param fileCreationMode Strategy such as [FileCreationMode.Overwrite] (default).
         * @return This builder for chaining.
         */
        fun setFileCreationMode(fileCreationMode: FileCreationMode): Builder {
            this.creationMode = fileCreationMode
            return this
        }

        /**
         * Registers a listener for download lifecycle and progress callbacks.
         *
         * @param downloadListener Callbacks invoked on the main thread unless documented otherwise.
         * @return This builder for chaining.
         */
        fun setDownloadListener(downloadListener: DownloadListener): Builder {
            this.downloadListener = downloadListener
            return this
        }

        /**
         * Creates an immutable [DownloadManager] with the configured options.
         *
         * @return A ready-to-use manager; call [DownloadManager.startDownload] to begin.
         * @throws IllegalArgumentException when download data is missing or both modes are configured.
         */
        fun build(): DownloadManager {
            validateConfiguration()

            return DownloadManager(
                context = context,
                downloadId = createDownloadId(),
                singleItemDownloadData = singleItemDownloadData,
                multipartItemDownloadData = multipartItemDownloadData,
                fileDirectories = directories,
                downloadType = downloadType,
                fileLocation = fileLocation,
                creationMode = creationMode,
                downloadListener = downloadListener,
            )
        }

        private fun validateConfiguration() {
            require(singleItemDownloadData != null || multipartItemDownloadData != null) {
                "Download data is required. Call setSingleItemDownloadData() or setMultiPartsDownloadData()."
            }
            require(singleItemDownloadData == null || multipartItemDownloadData == null) {
                "Single-file and multi-part download data cannot both be set."
            }

            singleItemDownloadData?.let { single ->
                require(single.url.isNotBlank()) { "Single-file URL must not be blank." }
                require(single.fileName.isNotBlank()) { "Single-file fileName must not be blank." }
            }

            multipartItemDownloadData?.let { multipart ->
                require(multipart.itemParts.isNotEmpty()) {
                    "Multi-part download requires at least one part in itemParts."
                }
                require(multipart.itemParts.all { it.url.isNotBlank() }) {
                    "Each multi-part entry must have a non-blank URL."
                }
                require(multipart.itemParts.first().fileName.isNotBlank()) {
                    "The first part must specify a non-blank fileName for the final output file."
                }
            }
        }

        /**
         * Builds a stable id used as the WorkManager unique-work name and control key.
         */
        private fun createDownloadId(): String =
            when (downloadType) {
                DownloadType.SingleFile ->
                    DownloadIds.singleFile(singleItemDownloadData!!)

                DownloadType.MultiParts ->
                    DownloadIds.multipart(multipartItemDownloadData!!)
            }
    }
}
