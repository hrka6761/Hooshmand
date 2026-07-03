package ir.hrka.download.manager.internal.work

import androidx.work.Data
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.DownloadType
import ir.hrka.download.manager.FileCreationMode
import ir.hrka.download.manager.MultipartItemDownloadData
import ir.hrka.download.manager.SingleItemDownloadData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Immutable configuration passed from [ir.hrka.download.manager.api.DownloadManager] to the
 * background download worker via WorkManager [Data].
 *
 * WorkManager only accepts primitive bundles, so complex fields (directories, headers, multipart
 * parts) are serialized to JSON strings. The worker deserializes this object at startup and uses
 * it to resolve the output file and download strategy.
 *
 * @property downloadId Stable identifier for this job; used as a WorkManager tag and notification id.
 * @property downloadType Whether the job is a single URL or sequential multipart append.
 * @property url Remote URL for [DownloadType.SingleFile]; `null` for multipart jobs.
 * @property parts Ordered part list for [DownloadType.MultiParts]; empty for single-file jobs.
 * Each part is downloaded in order and appended to the same output [fileName].
 * @property fileName Final on-disk file name (including extension).
 * @property directories Nested subdirectory segments under the selected storage location.
 * @property fileSize For [DownloadType.SingleFile], the file content size. For
 * [DownloadType.MultiParts], the combined total size (sum of known part sizes).
 * @property headers HTTP headers for single-file downloads; empty for multipart (headers live on each part).
 * @property checksum Optional integrity checksum for the final output file (single-file only).
 * @property storageLocation Target storage area.
 * @property creationMode File collision strategy.
 */
internal data class DownloadWorkInput(
    val downloadId: String,
    val downloadType: DownloadType,
    val url: String?,
    val parts: List<SingleItemDownloadData>,
    val fileName: String,
    val directories: List<String>,
    val fileSize: Long?,
    val headers: Map<String, String>,
    val checksum: String?,
    val storageLocation: DownloadStorageLocation,
    val creationMode: FileCreationMode,
) {

    /**
     * Serializes this input into WorkManager [Data] for [androidx.work.OneTimeWorkRequestBuilder].
     */
    fun toWorkData(): Data = Data.Builder()
        .putString(KEY_DOWNLOAD_ID, downloadId)
        .putString(KEY_DOWNLOAD_TYPE, downloadType.name)
        .putString(KEY_URL, url)
        .putString(KEY_PARTS_JSON, encodeParts(parts))
        .putString(KEY_FILE_NAME, fileName)
        .putString(KEY_DIRECTORIES_JSON, encodeStringList(directories))
        .putLong(KEY_FILE_SIZE, fileSize ?: INVALID_SIZE)
        .putString(KEY_HEADERS_JSON, encodeHeaders(headers))
        .putString(KEY_CHECKSUM, checksum)
        .putString(KEY_STORAGE_LOCATION, storageLocation.name)
        .putString(KEY_CREATION_MODE, creationMode.name)
        .build()

    companion object {

        /** Prefix for WorkManager tags associated with a download job. */
        const val WORK_TAG_PREFIX = "download_manager"

        /** Sentinel [Data] long value when [fileSize] is unknown. */
        const val INVALID_SIZE = -1L

        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_DOWNLOAD_TYPE = "download_type"
        private const val KEY_URL = "url"
        private const val KEY_PARTS_JSON = "parts_json"
        private const val KEY_FILE_NAME = "file_name"
        private const val KEY_DIRECTORIES_JSON = "directories_json"
        private const val KEY_FILE_SIZE = "file_size"
        private const val KEY_HEADERS_JSON = "headers_json"
        private const val KEY_CHECKSUM = "checksum"
        private const val KEY_STORAGE_LOCATION = "storage_location"
        private const val KEY_CREATION_MODE = "creation_mode"

        /**
         * Builds [DownloadWorkInput] from [DownloadManager] builder configuration.
         *
         * @param downloadId Unique job identifier generated at enqueue time.
         * @param downloadType Resolved download mode.
         * @param singleItemDownloadData Populated for [DownloadType.SingleFile].
         * @param multipartItemDownloadData Populated for [DownloadType.MultiParts].
         * @param directories Output subdirectory segments.
         * @param storageLocation Target storage location.
         * @param creationMode File collision strategy.
         */
        fun fromDownloadConfig(
            downloadId: String,
            downloadType: DownloadType,
            singleItemDownloadData: SingleItemDownloadData?,
            multipartItemDownloadData: MultipartItemDownloadData?,
            directories: List<String>,
            storageLocation: DownloadStorageLocation,
            creationMode: FileCreationMode,
        ): DownloadWorkInput = when (downloadType) {
            DownloadType.SingleFile -> {
                val single = singleItemDownloadData
                    ?: throw IllegalArgumentException("singleItemDownloadData is required for SingleFile")
                DownloadWorkInput(
                    downloadId = downloadId,
                    downloadType = downloadType,
                    url = single.url,
                    parts = emptyList(),
                    fileName = single.fileName,
                    directories = directories,
                    fileSize = single.fileSize,
                    headers = single.headers,
                    checksum = single.checksum,
                    storageLocation = storageLocation,
                    creationMode = creationMode,
                )
            }

            DownloadType.MultiParts -> {
                val multipart = multipartItemDownloadData
                    ?: throw IllegalArgumentException("multipartItemDownloadData is required for MultiParts")
                val parts = multipart.itemParts
                require(parts.isNotEmpty()) { "MultiParts requires at least one part" }

                DownloadWorkInput(
                    downloadId = downloadId,
                    downloadType = downloadType,
                    url = null,
                    parts = parts,
                    fileName = parts.first().fileName,
                    directories = directories,
                    fileSize = totalSizeOfParts(parts),
                    headers = emptyMap(),
                    checksum = null,
                    storageLocation = storageLocation,
                    creationMode = creationMode,
                )
            }
        }

        /**
         * Restores [DownloadWorkInput] from worker [inputData].
         *
         * @throws IllegalArgumentException if required fields are missing or malformed.
         */
        fun fromWorkData(data: Data): DownloadWorkInput {
            val downloadId = data.getString(KEY_DOWNLOAD_ID)
                ?: throw IllegalArgumentException("Missing $KEY_DOWNLOAD_ID")
            val downloadType = data.getString(KEY_DOWNLOAD_TYPE)?.let { DownloadType.valueOf(it) }
                ?: throw IllegalArgumentException("Missing or invalid $KEY_DOWNLOAD_TYPE")
            val fileName = data.getString(KEY_FILE_NAME)
                ?: throw IllegalArgumentException("Missing $KEY_FILE_NAME")
            val storageLocation = data.getString(KEY_STORAGE_LOCATION)
                ?.let { DownloadStorageLocation.valueOf(it) }
                ?: throw IllegalArgumentException("Missing or invalid $KEY_STORAGE_LOCATION")
            val creationMode = data.getString(KEY_CREATION_MODE)
                ?.let { FileCreationMode.valueOf(it) }
                ?: throw IllegalArgumentException("Missing or invalid $KEY_CREATION_MODE")

            val rawFileSize = data.getLong(KEY_FILE_SIZE, INVALID_SIZE)
            val fileSize = rawFileSize.takeIf { it != INVALID_SIZE }

            return DownloadWorkInput(
                downloadId = downloadId,
                downloadType = downloadType,
                url = data.getString(KEY_URL),
                parts = decodeParts(data.getString(KEY_PARTS_JSON)),
                fileName = fileName,
                directories = decodeStringList(data.getString(KEY_DIRECTORIES_JSON)),
                fileSize = fileSize,
                headers = decodeHeaders(data.getString(KEY_HEADERS_JSON)),
                checksum = data.getString(KEY_CHECKSUM),
                storageLocation = storageLocation,
                creationMode = creationMode,
            )
        }

        /** Builds a WorkManager tag for [downloadId]. */
        fun workTag(downloadId: String): String = WORK_TAG_PREFIX + downloadId

        /** Returns the sum of known part sizes, or `null` when any part size is unknown. */
        private fun totalSizeOfParts(parts: List<SingleItemDownloadData>): Long? {
            if (parts.any { it.fileSize == null }) return null
            return parts.sumOf { it.fileSize ?: 0L }
        }

        private fun encodeStringList(values: List<String>): String =
            JSONArray(values).toString()

        private fun decodeStringList(json: String?): List<String> {
            if (json.isNullOrBlank()) return emptyList()
            val array = JSONArray(json)
            return buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        }

        private fun encodeHeaders(headers: Map<String, String>): String {
            val json = JSONObject()
            headers.forEach { (key, value) -> json.put(key, value) }
            return json.toString()
        }

        private fun decodeHeaders(json: String?): Map<String, String> {
            if (json.isNullOrBlank()) return emptyMap()
            val objectJson = JSONObject(json)
            return buildMap {
                objectJson.keys().forEach { key ->
                    put(key, objectJson.getString(key))
                }
            }
        }

        private fun encodeParts(parts: List<SingleItemDownloadData>): String {
            val array = JSONArray()
            parts.forEach { part ->
                array.put(
                    JSONObject()
                        .put("url", part.url)
                        .put("fileName", part.fileName)
                        .put("fileSize", part.fileSize ?: INVALID_SIZE)
                        .put("headers", JSONObject(part.headers))
                        .put("checksum", part.checksum),
                )
            }
            return array.toString()
        }

        private fun decodeParts(json: String?): List<SingleItemDownloadData> {
            if (json.isNullOrBlank()) return emptyList()
            val array = JSONArray(json)
            return buildList {
                for (index in 0 until array.length()) {
                    val partJson = array.getJSONObject(index)
                    val rawSize = partJson.optLong("fileSize", INVALID_SIZE)
                    val partHeadersJson = partJson.optJSONObject("headers")
                    val partHeaders = buildMap {
                        partHeadersJson?.keys()?.forEach { key ->
                            put(key, partHeadersJson.getString(key))
                        }
                    }
                    add(
                        SingleItemDownloadData(
                            url = partJson.getString("url"),
                            fileName = partJson.optString("fileName", ""),
                            fileSize = rawSize.takeIf { it != INVALID_SIZE },
                            headers = partHeaders,
                            checksum = partJson.optString("checksum").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
        }
    }
}
