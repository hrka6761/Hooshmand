plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.room)
    alias(libs.plugins.hrka.android.hilt)
}

android {
    namespace = "ir.hrka.database"
}
