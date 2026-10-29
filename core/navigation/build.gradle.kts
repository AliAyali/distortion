plugins {
    alias(libs.plugins.distortion.android.library)
    alias(libs.plugins.distortion.android.library.compose)
}

android {
    namespace = "com.aliayali.navigation"
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}