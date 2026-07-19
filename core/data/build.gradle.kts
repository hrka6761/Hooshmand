plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hrka.android.hilt)
}

android {
    namespace = "ir.hrka.hooshmand.data"
}

dependencies {
    api(projects.core.model)
    api(projects.core.network)
    api(projects.core.database)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
