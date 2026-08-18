plugins {
    id("vayou.android.library")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.feature.network"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.smb)
    implementation(projects.core.ui)

    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.coil.compose)
}
