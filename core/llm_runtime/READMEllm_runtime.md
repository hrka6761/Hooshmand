# `:core:llm_runtime`

Standalone Android library for on-device LLM inference using [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM).

This module is **pure logic** — no UI, no Hilt, no feature dependencies. It can be copied into other Android projects.

## Usage

```kotlin
val runtime = LlmRuntimeFactory.create(context)

runtime.initialize(
    LlmRuntimeConfig(
        modelPath = "/path/to/model.litertlm",
        accelerator = LlmAccelerator.GPU,
        maxTokens = 4096,
        topK = 64,
        topP = 0.95f,
        temperature = 1.0f,
        systemInstruction = "You are a helpful assistant.",
    ),
)

runtime.generate(
    request = LlmInferenceRequest(prompt = "Hello!"),
    callback = object : LlmGenerationCallback {
        override fun onToken(token: String) { /* stream to UI */ }
        override fun onThinking(thinking: String) { /* optional reasoning */ }
        override fun onComplete() { /* done */ }
        override fun onError(error: LlmRuntimeException) { /* handle */ }
    },
)

runtime.close()
```

### Flow-based streaming

```kotlin
runtime.generateFlow(LlmInferenceRequest(prompt = "Hello!"))
    .collect { event ->
        when (event) {
            is LlmGenerationEvent.Token -> println(event.text)
            is LlmGenerationEvent.Thinking -> println(event.text)
            LlmGenerationEvent.Done -> Unit
            is LlmGenerationEvent.Error -> throw event.exception
        }
    }
```

## Public API

| Type | Purpose |
|------|---------|
| `LlmRuntime` | Main inference interface |
| `LlmRuntimeFactory` | Creates runtime instances |
| `LlmRuntimeConfig` | Model path, accelerator, sampler settings |
| `LlmInferenceRequest` | Prompt and optional multimodal input |
| `LlmHistoryMessage` / `LlmHistoryRole` | Prior turns for seeding a session |
| `LlmGenerationCallback` | Token streaming callback |
| `LlmGenerationEvent` | Flow events for streaming |
| `LlmAccelerator` | CPU / GPU / NPU / TPU |
| `LlmRuntimeState` | Lifecycle state |
| `LlmRuntimeException` | Typed error hierarchy |

### Restoring conversation context

After [initialize], call [LlmRuntime.resetConversation] with saved turns so the model remembers prior messages when reopening a chat:

```kotlin
runtime.resetConversation(
    systemInstruction = "You are a helpful assistant.",
    initialMessages = listOf(
        LlmHistoryMessage(LlmHistoryRole.USER, "My name is Hamid"),
        LlmHistoryMessage(LlmHistoryRole.MODEL, "Nice to meet you, Hamid!"),
    ),
)
```

Uses LiteRT `ConversationConfig.initialMessages` under the hood (same approach as Google AI Edge Gallery).

## Model format

Requires a `.litertlm` model file on local storage. Download and path resolution are the host app's responsibility (e.g. via `:core:download_manager`).
