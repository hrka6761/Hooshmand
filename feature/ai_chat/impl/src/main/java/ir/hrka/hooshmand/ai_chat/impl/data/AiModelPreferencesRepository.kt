package ir.hrka.hooshmand.ai_chat.impl.data

import ir.hrka.datastore.api.PrimitiveDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Persists AI chat model download status and file path.
 */
@Singleton
class AiModelPreferencesRepository
@Inject
constructor(
    private val dataStore: PrimitiveDataStore,
    private val modelFileLocator: AiModelFileLocator,
) {
    val isModelDownloaded: Flow<Boolean> =
        dataStore.booleanFlow(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, default = false)

    val modelFilePath: Flow<String> =
        dataStore.stringFlow(AiModelPreferenceKeys.MODEL_FILE_PATH, default = "")

    suspend fun getIsModelDownloaded(): Boolean =
        dataStore.getBoolean(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, default = false)

    suspend fun getModelFilePath(): String =
        dataStore.getString(AiModelPreferenceKeys.MODEL_FILE_PATH, default = "")

    /**
     * Returns whether the model file is available to use.
     *
     * 1. Read the path from preferences and verify it points to a complete valid model file.
     * 2. If not, scan internal and public storage for a valid model file.
     * 3. When a valid file is found outside preferences, persist its path for later use.
     */
    suspend fun isModelReady(): Boolean {
        val storedPath = getModelFilePath()
        val validPath = modelFileLocator.resolveValidModelPath(storedPath) ?: return false
        if (validPath != storedPath)
            saveDownloadSuccess(filePath = validPath)

        return true
    }

    suspend fun saveDownloadSuccess(filePath: String) {
        dataStore.putBoolean(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, value = true)
        dataStore.putString(AiModelPreferenceKeys.MODEL_FILE_PATH, value = filePath)
    }

    suspend fun clearDownloadState() {
        dataStore.putBoolean(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, value = false)
        dataStore.putString(AiModelPreferenceKeys.MODEL_FILE_PATH, value = "")
    }
}
