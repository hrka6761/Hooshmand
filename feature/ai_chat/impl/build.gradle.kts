plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hrka.android.hilt)
    alias(libs.plugins.hooshmand.feature.impl)
}

android {
    namespace = "ir.hrka.hooshmand.ai_chat.impl"
}

dependencies {
    implementation(projects.feature.aiChat.api)
    implementation(projects.core.downloadManager)
    implementation(projects.core.datastore)
    implementation(projects.core.llmRuntime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.mikepenz.markdown.renderer.m3)
}
