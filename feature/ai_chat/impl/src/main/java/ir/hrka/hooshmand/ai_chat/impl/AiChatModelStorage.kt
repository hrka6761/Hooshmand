package ir.hrka.hooshmand.ai_chat.impl

/**
 * Local storage layout for the on-device AI chat model.
 *
 * Download URLs and file names come from remote `model.json` via [ir.hrka.hooshmand.data.repository.ModelManifestRepository].
 */
object AiChatModelStorage {

    /** Subdirectory under the chosen storage location. */
    const val MODEL_DIRECTORY: String = "hooshmand_app_ai_models"
}
