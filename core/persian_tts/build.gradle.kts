plugins {
    alias(libs.plugins.hrka.android.library)
}

android {
    namespace = "ir.hrka.persian.tts"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    packaging {
        jniLibs {
            // Keep packaged sherpa / onnxruntime shared libraries.
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Kotlin/Java OfflineTts API classes from the official sherpa-onnx Android AAR.
    // Native .so files live in src/main/jniLibs (extracted from the same AAR).
    api(files("libs/sherpa-onnx-classes.jar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
