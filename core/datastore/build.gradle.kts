plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.hilt)
}

android {
    namespace = "ir.hrka.datastore"
}

dependencies {
    api(libs.androidx.datastore)
    api(libs.androidx.datastore.preferences)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
