plugins {
    alias(libs.plugins.hrka.android.library)
}

android {
    namespace = "ir.hrka.download.manager"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {

    // AndroidX — platform utilities (Context extensions, notification compat helpers)
    implementation(libs.androidx.core.ktx)

    // Background execution — WorkManager foreground downloads that survive app process death
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines — suspend workers, progress dispatching, manager async flows
    implementation(libs.kotlinx.coroutines.android)

    // Networking — OkHttp resumable downloads
    implementation(libs.okhttp)

    // Dependency Injection — optional Hilt bindings; host app supplies the compiler plugin
    compileOnly(libs.hilt.android)

    // Unit tests
    testImplementation(libs.junit)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
