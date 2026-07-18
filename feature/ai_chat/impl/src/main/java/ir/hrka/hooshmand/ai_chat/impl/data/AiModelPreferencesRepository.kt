package ir.hrka.hooshmand.ai_chat.impl.data

import ir.hrka.datastore.api.PrimitiveDataStore
import ir.hrka.hooshmand.ai_chat.impl.AiChatModelSettings
import ir.hrka.llm.runtime.api.LlmAccelerator
import ir.hrka.llm.runtime.api.LlmRuntimeConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Persists AI chat model download status, file path, and user model settings.
 *
 * @property dataStore Primitive preference store used for all AI chat keys.
 * @property modelFileLocator Resolves a valid on-disk model when preferences are stale.
 */
@Singleton
class AiModelPreferencesRepository
@Inject
constructor(
    private val dataStore: PrimitiveDataStore,
    private val modelFileLocator: AiModelFileLocator,
) {
    /** Flow of whether a successful model download was recorded. */
    val isModelDownloaded: Flow<Boolean> =
        dataStore.booleanFlow(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, default = false)

    /** Flow of the stored absolute model file path. */
    val modelFilePath: Flow<String> =
        dataStore.stringFlow(AiModelPreferenceKeys.MODEL_FILE_PATH, default = "")

    /**
     * Returns whether a successful model download was recorded.
     *
     * @return `true` when the download-success flag is set.
     */
    suspend fun getIsModelDownloaded(): Boolean =
        dataStore.getBoolean(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, default = false)

    /**
     * Returns the stored absolute model file path.
     *
     * @return Path string, or empty when unset.
     */
    suspend fun getModelFilePath(): String =
        dataStore.getString(AiModelPreferenceKeys.MODEL_FILE_PATH, default = "")

    /**
     * Returns whether the model file is available to use.
     *
     * 1. Read the path from preferences and verify it points to a complete valid model file.
     * 2. If not, scan internal and public storage for a valid model file.
     * 3. When a valid file is found outside preferences, persist its path for later use.
     *
     * @return `true` when a valid complete model file exists on disk.
     */
    suspend fun isModelReady(): Boolean {
        val storedPath = getModelFilePath()
        val validPath = modelFileLocator.resolveValidModelPath(storedPath) ?: return false
        if (validPath != storedPath) {
            saveDownloadSuccess(filePath = validPath)
        }
        return true
    }

    /**
     * Marks the model as downloaded and stores its absolute [filePath].
     *
     * @param filePath Absolute path to the completed `.litertlm` file.
     */
    suspend fun saveDownloadSuccess(filePath: String) {
        dataStore.putBoolean(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, value = true)
        dataStore.putString(AiModelPreferenceKeys.MODEL_FILE_PATH, value = filePath)
    }

    /**
     * Clears persisted download status and model path.
     */
    suspend fun clearDownloadState() {
        dataStore.putBoolean(AiModelPreferenceKeys.IS_MODEL_DOWNLOADED, value = false)
        dataStore.putString(AiModelPreferenceKeys.MODEL_FILE_PATH, value = "")
    }

    /**
     * Loads persisted [AiChatModelSettings], falling back to defaults when unset or invalid.
     *
     * @return Settings restored from DataStore.
     */
    suspend fun getModelSettings(): AiChatModelSettings {
        val defaults = AiChatModelSettings()
        val acceleratorName =
            dataStore.getString(
                AiModelPreferenceKeys.MODEL_ACCELERATOR,
                default = defaults.accelerator.name,
            )
        val accelerator =
            runCatching { LlmAccelerator.valueOf(acceleratorName) }
                .getOrDefault(defaults.accelerator)

        val systemInstruction =
            dataStore
                .getString(AiModelPreferenceKeys.MODEL_SYSTEM_INSTRUCTION, default = "")
                .ifBlank { null }

        return AiChatModelSettings(
            accelerator = accelerator,
            maxTokens =
                dataStore.getInt(
                    AiModelPreferenceKeys.MODEL_MAX_TOKENS,
                    default = LlmRuntimeConfig.DEFAULT_MAX_TOKENS,
                ),
            topK =
                dataStore.getInt(
                    AiModelPreferenceKeys.MODEL_TOP_K,
                    default = LlmRuntimeConfig.DEFAULT_TOP_K,
                ),
            topP =
                dataStore.getFloat(
                    AiModelPreferenceKeys.MODEL_TOP_P,
                    default = LlmRuntimeConfig.DEFAULT_TOP_P,
                ),
            temperature =
                dataStore.getFloat(
                    AiModelPreferenceKeys.MODEL_TEMPERATURE,
                    default = LlmRuntimeConfig.DEFAULT_TEMPERATURE,
                ),
            systemInstruction = systemInstruction,
        )
    }

    /**
     * Persists [settings] so they survive process death and ViewModel recreation.
     *
     * @param settings User-confirmed model settings to store.
     */
    suspend fun saveModelSettings(settings: AiChatModelSettings) {
        dataStore.putString(
            AiModelPreferenceKeys.MODEL_ACCELERATOR,
            value = settings.accelerator.name,
        )
        dataStore.putInt(AiModelPreferenceKeys.MODEL_MAX_TOKENS, value = settings.maxTokens)
        dataStore.putInt(AiModelPreferenceKeys.MODEL_TOP_K, value = settings.topK)
        dataStore.putFloat(AiModelPreferenceKeys.MODEL_TOP_P, value = settings.topP)
        dataStore.putFloat(AiModelPreferenceKeys.MODEL_TEMPERATURE, value = settings.temperature)
        dataStore.putString(
            AiModelPreferenceKeys.MODEL_SYSTEM_INSTRUCTION,
            value = settings.systemInstruction.orEmpty(),
        )
    }
}
