# `:core:persian_tts`

Standalone Android library for **offline Persian (Farsi) text-to-speech** via
**sherpa-onnx** + **Piper** `fa_IR-gyro-medium` (int8). No Hooshmand-specific
dependencies beyond AndroidX / Coroutines — drop it into any app.

## Package layout

```
ir.hrka.persian.tts/
├── api/                      Public façade
│   ├── PersianTts            Engine interface (speak / stop / shutdown + StateFlows)
│   ├── PersianTtsFactory     Creates engine instances
│   └── PersianTtsResult      Sealed speak outcomes
└── internal/
    ├── SherpaPersianTts      sherpa-onnx + AudioTrack implementation
    └── PersianTtsAssetInstaller  Copies model/espeak assets to filesDir once
```

## Languages

This module ships **one** Piper voice: `fa_IR-gyro-medium` (Persian / Farsi only).
It is **not** a multilingual TTS engine.

| Language | Supported by this module |
|----------|--------------------------|
| Persian (`fa`) | Yes |
| Arabic, English, others | No — use system `TextToSpeech` or another model |

To support another language you would need to replace/add a matching Piper (or
other sherpa-onnx) voice; the public API is intentionally Persian-focused.

## Setup (host app)

1. Include the module in Gradle:

   ```kotlin
   // settings.gradle.kts
   include(":core:persian_tts")

   // app/build.gradle.kts
   implementation(project(":core:persian_tts"))
   ```

2. No special `Application` hooks or permissions are required for playback of
   synthesized audio through `AudioTrack` (media usage).

3. Keep ProGuard consumer rules shipped by the module
   (`consumer-rules.pro`) so `com.k2fsa.sherpa.onnx.**` is not stripped.

## Usage

```kotlin
val tts = PersianTtsFactory.create(context)

// Optional: wait until the model is ready
lifecycleScope.launch {
    tts.isReady.first { it }

    when (
        tts.speakOrStop(
            utteranceId = "welcome",
            text = "سلام! به هوشمند خوش آمدید.",
        )
    ) {
        PersianTtsResult.Ok -> Unit
        PersianTtsResult.EngineNotReady -> { /* still loading / shut down */ }
        PersianTtsResult.EmptyText -> Unit
        is PersianTtsResult.SpeakFailed -> {
            Log.e("Tts", it.message, it.cause)
        }
    }
}

// Toggle: calling speakOrStop again with the same utteranceId while speaking stops.
// Or explicitly:
tts.stop()

// When the screen / process no longer needs TTS:
tts.shutdown()
```

### Observing playback state

```kotlin
tts.isReady.collect { ready -> /* enable speak button */ }
tts.isSpeaking.collect { speaking -> /* show stop affordance */ }
tts.speakingUtteranceId.collect { id -> /* highlight message id */ }
```

## Bundled assets

Installed from `src/main/assets/persian_tts/` into `filesDir/persian_tts/` on
first use:

```
persian_tts/
├── model/
│   ├── fa_IR-gyro-medium.onnx
│   └── tokens.txt
└── espeak-ng-data/          # Piper phonemizer data (fa + shared)
```

Native engine (from [sherpa-onnx releases](https://github.com/k2-fsa/sherpa-onnx/releases)):

```
libs/sherpa-onnx-classes.jar
src/main/jniLibs/<abi>/libonnxruntime.so
src/main/jniLibs/<abi>/libsherpa-onnx-jni.so
```

### Updating the sherpa-onnx engine

1. Download `sherpa-onnx-<version>.aar`.
2. Extract `classes.jar` → `libs/sherpa-onnx-classes.jar`.
3. Extract `jni/<abi>/*.so` → `src/main/jniLibs/<abi>/`.
4. Optionally keep the full AAR under `libs/` for reference.

### Updating the Persian voice

Replace `model/fa_IR-gyro-medium.onnx` and matching `tokens.txt`, bump the
install marker in `PersianTtsAssetInstaller` (e.g. `.ready_v2`) so devices
re-copy assets, and rebuild.

## Behavior notes

- **Markdown**: light markdown (fences, links, emphasis, list markers) is stripped
  before synthesis so the voice reads prose.
- **Speed**: default generation speed is `0.85` for clearer Persian narration.
- **Latency / long text**: synthesis is done in short segments (sentence / ~140
  chars). Playback starts after the **first** segment is ready instead of waiting
  for the whole utterance to be generated.
- **Threading**: init, synthesize, and `AudioTrack.write` run on a dedicated
  worker; `stop()` can interrupt long playback.
- **JNI**: uses `OfflineTts.generate()` (not callback APIs) to avoid Kotlin/JNI
  lambda `NoSuchMethodError` issues on modern AGP/D8.

## Independence

- Package: `ir.hrka.persian.tts` (not app-specific)
- Dependencies: AndroidX Core KTX, Kotlin Coroutines, sherpa-onnx classes + JNI
- Host wires UI / lifecycle only; the module owns model install and audio playback
