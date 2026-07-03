package ir.hrka.download.manager

/**
 * Target on-device storage area for a downloaded file.
 *
 * Each value maps to a dedicated [ir.hrka.download.manager.filing.FileProvider]
 * implementation that resolves the output path and enforces storage permissions.
 *
 * @see DownloadManager.Builder.setFileLocation
 */
enum class DownloadStorageLocation {

    /**
     * App-private internal storage.
     *
     * Removed when the app is uninstalled.
     */
    Internal,

    /**
     * App-specific external storage.
     *
     * Removed when the app is uninstalled. Suitable for large app-managed files.
     */
    External,

    /**
     * Shared/public storage (e.g. MediaStore Downloads).
     *
     * Files remain on the device after the app is uninstalled and are accessible
     * to the user and other apps with appropriate permissions.
     */
    Public,
}
