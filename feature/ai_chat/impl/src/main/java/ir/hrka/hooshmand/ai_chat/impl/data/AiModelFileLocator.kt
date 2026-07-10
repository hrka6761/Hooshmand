package ir.hrka.hooshmand.ai_chat.impl.data

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.hooshmand.ai_chat.impl.AiChatModelDownloadParts
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
     */
    fun resolveValidModelPath(storedPath: String?): String? =
        candidateFiles(storedPath)
            .firstOrNull(::isValidCompleteModelFile)
            ?.absolutePath

    /** Resolves the expected model output file for [storageLocation]. */
    fun modelFileFor(storageLocation: DownloadStorageLocation): File? =
        when (storageLocation) {
            DownloadStorageLocation.Internal -> internalModelFile()
            DownloadStorageLocation.Public -> publicModelFile()
            DownloadStorageLocation.External -> null
        }

    /** Whether [file] is a complete model file with the expected name, type, and size. */
    fun isValidCompleteModelFile(file: File): Boolean {
        if (!file.isFile) return false
        if (!hasValidModelType(file)) return false
        val expectedSize = AiChatModelDownloadParts.totalSizeInBytes ?: return false
        return file.length() == expectedSize
    }

    /**
     * Whether an existing file at the download target should be overwritten instead of appended.
     *
     * Partial downloads with the correct type can resume via append; invalid type or oversized
     * files must be replaced.
     */
    fun shouldOverwriteExistingFile(file: File): Boolean {
        if (!file.exists()) return false
        if (isValidCompleteModelFile(file)) return false
        if (!hasValidModelType(file)) return true
        val expectedSize = AiChatModelDownloadParts.totalSizeInBytes ?: return false
        return file.length() > expectedSize
    }


    private fun candidateFiles(storedPath: String?): List<File> =
        buildList {
            if (!storedPath.isNullOrBlank()) {
                add(File(storedPath))
            }
            add(internalModelFile())
            publicModelFile()?.let(::add)
        }.distinctBy { it.absolutePath }

    private fun internalModelFile(): File =
        File(
            File(context.filesDir, AiChatModelDownloadParts.MODEL_DIRECTORY),
            AiChatModelDownloadParts.MODEL_FILE_NAME,
        )

    private fun publicModelFile(): File? {
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (Environment.getExternalStorageState(baseDir) != Environment.MEDIA_MOUNTED) {
            return null
        }
        return File(
            File(baseDir, AiChatModelDownloadParts.MODEL_DIRECTORY),
            AiChatModelDownloadParts.MODEL_FILE_NAME,
        )
    }

    private fun hasValidModelType(file: File): Boolean {
        if (file.name != AiChatModelDownloadParts.MODEL_FILE_NAME) return false
        return file.name.endsWith(MODEL_FILE_EXTENSION)
    }

    private companion object {
        const val MODEL_FILE_EXTENSION: String = ".litertlm"
    }
}
