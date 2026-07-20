package ir.hrka.download.manager.model

/**
 * Strategy applied when a download target file already exists on disk.
 *
 * When a WorkManager job is **retried** after process death, [Overwrite] is automatically
 * treated as [Append] so partial progress is not deleted.
 *
 * @see ir.hrka.download.manager.api.DownloadManager.Builder.setFileCreationMode
 */
enum class FileCreationMode {

    /**
     * Delete the existing file before creating a new one on a fresh start.
     */
    Overwrite,

    /**
     * Keep the existing file and append data to it (resume-friendly).
     */
    Append,

    /**
     * Create a new file with a unique name so the existing file is left untouched.
     */
    CreateNew,
}
