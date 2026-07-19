plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hrka.android.hilt)
    alias(libs.plugins.hooshmand.feature.impl)
}

android {
    namespace = "ir.hrka.hooshmand.ai_chat_history.impl"
}

dependencies {
    implementation(projects.feature.aiChatHistory.api)
    implementation(projects.feature.aiChat.api)
    implementation(projects.core.data)
}
