plugins {
    alias(libs.plugins.hrka.android.application)
    alias(libs.plugins.hrka.android.app.compose)
    alias(libs.plugins.hrka.android.flavors)
    alias(libs.plugins.hrka.android.hilt)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation3
    implementation(libs.androidx.navigation3.ui)

    // Project modules
    implementation(projects.core.navigation)
    implementation(projects.core.downloadManager)
    implementation(projects.core.datastore)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(projects.feature.home.api)
    implementation(projects.feature.home.impl)
    implementation(projects.feature.aiChatHistory.api)
    implementation(projects.feature.aiChatHistory.impl)
    implementation(projects.feature.aiChat.api)
    implementation(projects.feature.aiChat.impl)
}
