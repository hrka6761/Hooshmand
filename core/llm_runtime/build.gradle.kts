plugins {
    alias(libs.plugins.hrka.android.library)
}

android {
    namespace = "ir.hrka.llm.runtime"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // LiteRT-LM on-device LLM engine
    implementation(libs.litertlm)
    implementation(libs.play.services.tflite.java)
    implementation(libs.play.services.tflite.gpu)
    implementation(libs.play.services.tflite.support)
}
