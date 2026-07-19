plugins {
    alias(libs.plugins.hrka.android.library)
}

android {
    namespace = "ir.hrka.hooshmand.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    implementation(libs.hilt.android)
}
