plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hooshmand.feature.api)
}

android {
    namespace = "ir.hrka.hooshmand.ai_chat_history.api"
}
