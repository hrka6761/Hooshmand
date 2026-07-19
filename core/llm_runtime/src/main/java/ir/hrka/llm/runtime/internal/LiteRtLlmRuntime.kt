package ir.hrka.llm.runtime.internal

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import ir.hrka.llm.runtime.api.LlmAccelerator
import ir.hrka.llm.runtime.api.LlmGenerationCallback
import ir.hrka.llm.runtime.api.LlmGenerationEvent
import ir.hrka.llm.runtime.api.LlmInferenceRequest
import ir.hrka.llm.runtime.api.LlmRuntime
import ir.hrka.llm.runtime.api.LlmRuntimeConfig
import ir.hrka.llm.runtime.api.LlmRuntimeState
import ir.hrka.llm.runtime.api.exception.LlmAlreadyClosedException
import ir.hrka.llm.runtime.api.exception.LlmGenerationInProgressException
import ir.hrka.llm.runtime.api.exception.LlmInferenceException
import ir.hrka.llm.runtime.api.exception.LlmInitializationException
import ir.hrka.llm.runtime.api.exception.LlmInvalidRequestException
import ir.hrka.llm.runtime.api.exception.LlmModelNotFoundException
import ir.hrka.llm.runtime.api.exception.LlmNotInitializedException
import ir.hrka.llm.runtime.api.exception.LlmRuntimeException
import java.io.File
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LiteRT-LM implementation of [LlmRuntime].
 *
 * Manages engine initialization, conversation lifecycle, and streaming inference
 * through the LiteRT-LM Android SDK.
 */
internal class LiteRtLlmRuntime(
    private val appContext: Context,
) : LlmRuntime {

    private val mutex = Mutex()

    @Volatile
    private var currentState: LlmRuntimeState = LlmRuntimeState.IDLE

    override val state: LlmRuntimeState
        get() = currentState

    private var activeConfig: LlmRuntimeConfig? = null
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override suspend fun initialize(config: LlmRuntimeConfig) {
        mutex.withLock {
            ensureNotClosed()
            if (currentState == LlmRuntimeState.GENERATING) {
                throw LlmGenerationInProgressException(
                    "Cannot initialize while inference is in progress.",
                )
            }

            validateModelFile(config.modelPath)

            currentState = LlmRuntimeState.INITIALIZING
            releaseEngineLocked()
        }

        // Release [mutex] during native load so ViewModel.onCleared can cancel/close without
        // deadlocking the main thread.
        var loadedEngine: Engine? = null
        var loadedConversation: Conversation? = null
        try {
            val loaded =
                withContext(Dispatchers.IO) {
                    createEngineAndConversation(config)
                }
            loadedEngine = loaded.engine
            loadedConversation = loaded.conversation

            mutex.withLock {
                if (currentState == LlmRuntimeState.CLOSED) {
                    releaseEngineResources(loaded.conversation, loaded.engine)
                    return
                }
                releaseEngineLocked()
                engine = loaded.engine
                conversation = loaded.conversation
                activeConfig = config
                currentState = LlmRuntimeState.READY
                loadedEngine = null
                loadedConversation = null
            }
        } catch (e: CancellationException) {
            releaseEngineResources(loadedConversation, loadedEngine)
            mutex.withLock {
                if (currentState == LlmRuntimeState.INITIALIZING) {
                    currentState = LlmRuntimeState.IDLE
                }
            }
            throw e
        } catch (e: LlmRuntimeException) {
            releaseEngineResources(loadedConversation, loadedEngine)
            mutex.withLock {
                if (currentState == LlmRuntimeState.INITIALIZING) {
                    currentState = LlmRuntimeState.IDLE
                }
                releaseEngineLocked()
            }
            throw e
        } catch (e: Exception) {
            releaseEngineResources(loadedConversation, loadedEngine)
            mutex.withLock {
                if (currentState == LlmRuntimeState.INITIALIZING) {
                    currentState = LlmRuntimeState.IDLE
                }
                releaseEngineLocked()
            }
            throw LlmInitializationException(
                message = sanitizeErrorMessage(e.message ?: "Unknown initialization error"),
                cause = e,
            )
        }
    }

    override suspend fun generate(request: LlmInferenceRequest, callback: LlmGenerationCallback) {
        mutex.withLock {
            ensureReady()
            if (currentState == LlmRuntimeState.GENERATING) {
                throw LlmGenerationInProgressException()
            }
            currentState = LlmRuntimeState.GENERATING
        }

        try {
            validateRequest(request)
            val activeConversation = conversation
                ?: throw LlmNotInitializedException("Conversation is not available.")

            val mergedContext = buildExtraContext(request)

            suspendCancellableCoroutine { continuation ->
                val contents = buildContents(request)

                activeConversation.sendMessageAsync(
                    Contents.of(contents),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            dispatchMessage(message, callback)
                        }

                        override fun onDone() {
                            callback.onComplete()
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }

                        override fun onError(throwable: Throwable) {
                            if (throwable is CancellationException) {
                                callback.onComplete()
                                if (continuation.isActive) {
                                    continuation.resume(Unit)
                                }
                                return
                            }

                            val error =
                                LlmInferenceException(
                                    message =
                                        sanitizeErrorMessage(
                                            throwable.message ?: "Unknown inference error",
                                        ),
                                    cause = throwable,
                                )
                            callback.onError(error)
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }
                    },
                    mergedContext,
                )

                continuation.invokeOnCancellation {
                    activeConversation.cancelProcess()
                }
            }
        } catch (e: LlmRuntimeException) {
            callback.onError(e)
        } catch (e: Exception) {
            val error =
                LlmInferenceException(
                    message = sanitizeErrorMessage(e.message ?: "Unknown inference error"),
                    cause = e,
                )
            callback.onError(error)
        } finally {
            mutex.withLock {
                if (currentState == LlmRuntimeState.GENERATING) {
                    currentState = LlmRuntimeState.READY
                }
            }
        }
    }

    override fun generateFlow(request: LlmInferenceRequest): Flow<LlmGenerationEvent> {
        return callbackFlow {
            try {
                generate(
                    request = request,
                    callback =
                        object : LlmGenerationCallback {
                            override fun onToken(token: String) {
                                trySend(LlmGenerationEvent.Token(token))
                            }

                            override fun onThinking(thinking: String) {
                                trySend(LlmGenerationEvent.Thinking(thinking))
                            }

                            override fun onComplete() {
                                trySend(LlmGenerationEvent.Done)
                                close()
                            }

                            override fun onError(error: LlmRuntimeException) {
                                trySend(LlmGenerationEvent.Error(error))
                                close(error)
                            }
                        },
                )
            } catch (e: LlmRuntimeException) {
                trySend(LlmGenerationEvent.Error(e))
                close(e)
            }

            awaitClose {
                stopGeneration()
            }
        }
    }

    override fun stopGeneration() {
        conversation?.cancelProcess()
    }

    @OptIn(ExperimentalApi::class)
    override suspend fun resetConversation(systemInstruction: String?) {
        mutex.withLock {
            ensureReady()
            if (currentState == LlmRuntimeState.GENERATING) {
                throw LlmGenerationInProgressException(
                    "Cannot reset conversation while inference is in progress.",
                )
            }

            val config = activeConfig ?: throw LlmNotInitializedException()
            val activeEngine = engine ?: throw LlmNotInitializedException()

            try {
                conversation?.close()
                conversation =
                    activeEngine.createConversation(
                        buildConversationConfig(
                            config = config,
                            systemInstruction = systemInstruction ?: config.systemInstruction,
                        ),
                    )
            } catch (e: Exception) {
                throw LlmInitializationException(
                    message = "Failed to reset conversation: ${sanitizeErrorMessage(e.message ?: "")}",
                    cause = e,
                )
            }
        }
    }

    override suspend fun close() {
        mutex.withLock {
            if (currentState == LlmRuntimeState.CLOSED) {
                return
            }
            if (currentState == LlmRuntimeState.GENERATING) {
                conversation?.cancelProcess()
            }
            releaseEngineLocked()
            activeConfig = null
            currentState = LlmRuntimeState.CLOSED
        }
    }

    @OptIn(ExperimentalApi::class)
    private fun createEngineAndConversation(config: LlmRuntimeConfig): LoadedRuntime {
        val nativeLibraryDir = appContext.applicationInfo.nativeLibraryDir
        val textBackend = config.accelerator.toBackend(nativeLibraryDir)
        val visionBackend =
            if (config.supportImage) {
                (config.visionAccelerator ?: config.accelerator).toBackend(nativeLibraryDir)
            } else {
                null
            }
        val audioBackend = if (config.supportAudio) Backend.CPU() else null

        val resolvedCacheDir =
            config.cacheDir
                ?: if (config.modelPath.startsWith("/data/local/tmp")) {
                    appContext.getExternalFilesDir(null)?.absolutePath
                } else {
                    null
                }

        val engineConfig =
            EngineConfig(
                modelPath = config.modelPath,
                backend = textBackend,
                visionBackend = visionBackend,
                audioBackend = audioBackend,
                maxNumTokens = config.maxTokens,
                cacheDir = resolvedCacheDir,
            )

        val supportsSpeculativeDecoding = detectSpeculativeDecodingSupport(config.modelPath)
        val enableSpeculativeDecoding =
            config.enableSpeculativeDecoding && supportsSpeculativeDecoding

        ExperimentalFlags.enableSpeculativeDecoding = enableSpeculativeDecoding
        val newEngine = Engine(engineConfig)
        try {
            newEngine.initialize()
        } finally {
            ExperimentalFlags.enableSpeculativeDecoding = false
        }

        val newConversation =
            newEngine.createConversation(
                buildConversationConfig(
                    config = config,
                    systemInstruction = config.systemInstruction,
                ),
            )

        return LoadedRuntime(engine = newEngine, conversation = newConversation)
    }

    @OptIn(ExperimentalApi::class)
    private fun buildConversationConfig(
        config: LlmRuntimeConfig,
        systemInstruction: String?,
    ): ConversationConfig {
        val instruction =
            systemInstruction?.takeIf { it.isNotBlank() }?.let { Contents.of(it) }

        val samplerConfig =
            if (config.accelerator.isNpuFamily()) {
                null
            } else {
                SamplerConfig(
                    topK = config.topK,
                    topP = config.topP.toDouble(),
                    temperature = config.temperature.toDouble(),
                )
            }

        return ConversationConfig(
            samplerConfig = samplerConfig,
            systemInstruction = instruction,
        )
    }

    private fun releaseEngineLocked() {
        releaseEngineResources(conversation, engine)
        conversation = null
        engine = null
    }

    private fun releaseEngineResources(
        conversationToClose: Conversation?,
        engineToClose: Engine?,
    ) {
        try {
            conversationToClose?.close()
        } catch (_: Exception) {
            // Best-effort cleanup.
        }

        try {
            engineToClose?.close()
        } catch (_: Exception) {
            // Best-effort cleanup.
        }
    }

    private data class LoadedRuntime(
        val engine: Engine,
        val conversation: Conversation,
    )

    private fun ensureNotClosed() {
        if (currentState == LlmRuntimeState.CLOSED) {
            throw LlmAlreadyClosedException()
        }
    }

    private fun ensureReady() {
        ensureNotClosed()
        if (currentState != LlmRuntimeState.READY && currentState != LlmRuntimeState.GENERATING) {
            throw LlmNotInitializedException()
        }
        if (engine == null || conversation == null) {
            throw LlmNotInitializedException("Engine or conversation is not available.")
        }
    }

    private fun validateModelFile(modelPath: String) {
        val file = File(modelPath)
        if (!file.exists()) {
            throw LlmModelNotFoundException(modelPath)
        }
        if (!file.isFile) {
            throw LlmModelNotFoundException("$modelPath (not a regular file)")
        }
        if (!file.canRead()) {
            throw LlmModelNotFoundException("$modelPath (not readable)")
        }
    }

    private fun validateRequest(request: LlmInferenceRequest) {
        try {
            LlmInferenceRequest(
                prompt = request.prompt,
                imageBytes = request.imageBytes,
                audioBytes = request.audioBytes,
                enableThinking = request.enableThinking,
                extraContext = request.extraContext,
            )
        } catch (e: IllegalArgumentException) {
            throw LlmInvalidRequestException(e.message ?: "Invalid inference request")
        }

        val config = activeConfig
        if (config != null) {
            if (request.imageBytes.isNotEmpty() && !config.supportImage) {
                throw LlmInvalidRequestException(
                    "Image input provided but runtime was not initialized with supportImage=true",
                )
            }
            if (request.audioBytes.isNotEmpty() && !config.supportAudio) {
                throw LlmInvalidRequestException(
                    "Audio input provided but runtime was not initialized with supportAudio=true",
                )
            }
        }
    }

    private fun buildContents(request: LlmInferenceRequest): List<Content> {
        val contents = mutableListOf<Content>()
        for (imageBytes in request.imageBytes) {
            if (imageBytes.isEmpty()) {
                throw LlmInvalidRequestException("Image byte array must not be empty")
            }
            contents.add(Content.ImageBytes(imageBytes))
        }
        for (audioBytes in request.audioBytes) {
            if (audioBytes.isEmpty()) {
                throw LlmInvalidRequestException("Audio byte array must not be empty")
            }
            contents.add(Content.AudioBytes(audioBytes))
        }
        if (request.prompt.isNotBlank()) {
            contents.add(Content.Text(request.prompt))
        }
        return contents
    }

    private fun buildExtraContext(request: LlmInferenceRequest): Map<String, String> {
        val context = request.extraContext.toMutableMap()
        if (request.enableThinking) {
            context["enable_thinking"] = "true"
        }
        return context
    }

    private fun dispatchMessage(message: Message, callback: LlmGenerationCallback) {
        val thinking = message.channels["thought"]
        if (!thinking.isNullOrEmpty()) {
            callback.onThinking(thinking)
        }

        val text = message.toString()
        if (text.isNotEmpty() && !text.startsWith("<ctrl")) {
            callback.onToken(text)
        }
    }

    private fun detectSpeculativeDecodingSupport(modelPath: String): Boolean {
        return try {
            com.google.ai.edge.litertlm.Capabilities(modelPath).use {
                it.hasSpeculativeDecodingSupport()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun LlmAccelerator.toBackend(nativeLibraryDir: String): Backend {
        return when (this) {
            LlmAccelerator.CPU -> Backend.CPU()
            LlmAccelerator.GPU -> Backend.GPU()
            LlmAccelerator.NPU,
            LlmAccelerator.TPU,
            -> Backend.NPU(nativeLibraryDir = nativeLibraryDir)
        }
    }

    private fun LlmAccelerator.isNpuFamily(): Boolean {
        return this == LlmAccelerator.NPU || this == LlmAccelerator.TPU
    }

    /**
     * Strips verbose native stack traces from error messages for cleaner reporting.
     */
    private fun sanitizeErrorMessage(message: String): String {
        val marker = "=== Source Location Trace"
        val index = message.indexOf(marker)
        return if (index >= 0) {
            message.substring(0, index).trimEnd()
        } else {
            message
        }
    }
}
