package ir.hrka.download.manager

/**
 * Strategy applied when a download target file already exists on disk.
 *
 * @see DownloadManager.Builder.setFileCreationMode
 * @see FileProvider
 */
enum class FileCreationMode {

    /**
     * Overwrite the existing file by deleting it before creating a new one.
     */
    Overwrite,

    /**
     * Use the existing file and append data to it without deleting.
     */
    Append,

    /**
     * Create a new file with a unique name, typically by appending a timestamp or other identifier,
     * to avoid overwriting the existing file.
     */
    CreateNew
}

