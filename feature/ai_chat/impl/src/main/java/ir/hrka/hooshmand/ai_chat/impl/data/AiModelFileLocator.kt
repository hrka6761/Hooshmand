package ir.hrka.hooshmand.ai_chat.impl.data

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.hooshmand.ai_chat.impl.AiChatModelStorage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locates and validates the on-device AI chat model file in internal and public storage.
 */
@Singleton
class AiModelFileLocator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /**
     * Returns the absolute path of a complete, valid model file if one exists.
     *
     * Checks the stored [storedPath] first, then internal storage, then public storage.
     *
     * @param storedPath Preferred path from preferences, or `null`.
     * @param expectedFileName Expected `.litertlm` file name from the remote catalog, or `null`
     * to accept any `.litertlm` under the model directory.
     */
    fun resolveValidModelPath(
        storedPath: String?,
        expectedFileName: String? = null,
    ): String? =
        candidateFiles(storedPath = storedPath, expectedFileName = expectedFileName)
            .firstOrNull { isValidCompleteModelFile(file = it, expectedFileName = expectedFileName) }
            ?.absolutePath

    /**
     * Resolves the expected model output file for [storageLocation].
     *
     * @param storageLocation Chosen download storage.
     * @param modelFileName Output file name from the remote catalog.
     */
    fun modelFileFor(
        storageLocation: DownloadStorageLocation,
        modelFileName: String,
    ): File? =
        when (storageLocation) {
            DownloadStorageLocation.Internal -> internalModelFile(modelFileName)
            DownloadStorageLocation.Public -> publicModelFile(modelFileName)
            DownloadStorageLocation.External -> null
        }

    /**
     * Whether [file] is a usable model file.
     *
     * Without a known expected size from the remote catalog, any non-empty `.litertlm` with the
     * expected name (when provided) is accepted.
     *
     * @param file Candidate file.
     * @param expectedFileName Expected file name, or `null` to skip the name check.
     */
    fun isValidCompleteModelFile(
        file: File,
        expectedFileName: String? = null,
    ): Boolean {
        if (!file.isFile) return false
        if (!file.name.endsWith(MODEL_FILE_EXTENSION)) return false
        if (expectedFileName != null && file.name != expectedFileName) return false
        return file.length() > 0L
    }

    /**
     * Whether [absolutePath] is under the shared public Downloads tree used for public models.
     *
     * @param absolutePath Absolute filesystem path to check.
     * @return `true` when the path is inside public Downloads.
     */
    fun isUnderPublicStorage(absolutePath: String): Boolean {
        if (absolutePath.isBlank()) return false
        val publicBase =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return absolutePath.startsWith(publicBase.absolutePath)
    }

    /**
     * Whether reading [absolutePath] requires all-files access that is not currently granted.
     *
     * @param absolutePath Absolute filesystem path of the model file.
     * @return `true` when the path is public and manage-all-files access is missing.
     */
    fun requiresPublicStoragePermission(absolutePath: String): Boolean =
        isUnderPublicStorage(absolutePath) && !Environment.isExternalStorageManager()

    /**
     * Whether an existing file at the download target should be overwritten instead of appended.
     *
     * @param file Target output file.
     * @param expectedFileName Expected model file name.
     */
    fun shouldOverwriteExistingFile(
        file: File,
        expectedFileName: String,
    ): Boolean {
        if (!file.exists()) return false
        if (isValidCompleteModelFile(file = file, expectedFileName = expectedFileName)) {
            return false
        }
        if (!file.name.endsWith(MODEL_FILE_EXTENSION)) return true
        return false
    }

    private fun candidateFiles(
        storedPath: String?,
        expectedFileName: String?,
    ): List<File> =
        buildList {
            if (!storedPath.isNullOrBlank()) {
                add(File(storedPath))
            }
            if (!expectedFileName.isNullOrBlank()) {
                add(internalModelFile(expectedFileName))
                publicModelFile(expectedFileName)?.let(::add)
            } else {
                listLitertlmFiles(internalModelDirectory()).forEach(::add)
                publicModelDirectory()?.let { dir ->
                    listLitertlmFiles(dir).forEach(::add)
                }
            }
        }.distinctBy { it.absolutePath }

    private fun internalModelDirectory(): File =
        File(context.filesDir, AiChatModelStorage.MODEL_DIRECTORY)

    private fun publicModelDirectory(): File? {
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (Environment.getExternalStorageState(baseDir) != Environment.MEDIA_MOUNTED) {
            return null
        }
        return File(baseDir, AiChatModelStorage.MODEL_DIRECTORY)
    }

    private fun internalModelFile(modelFileName: String): File =
        File(internalModelDirectory(), modelFileName)

    private fun publicModelFile(modelFileName: String): File? {
        val directory = publicModelDirectory() ?: return null
        return File(directory, modelFileName)
    }

    private fun listLitertlmFiles(directory: File): List<File> {
        if (!directory.isDirectory) return emptyList()
        return directory
            .listFiles { file -> file.isFile && file.name.endsWith(MODEL_FILE_EXTENSION) }
            ?.toList()
            .orEmpty()
    }

    private companion object {
        const val MODEL_FILE_EXTENSION: String = ".litertlm"
    }
}
