plugins {
    id("vayou.android.library")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.feature.settings"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.media)
    implementation(projects.core.model)
    implementation(projects.core.ui)

    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.hilt.navigation.compose)
    // Where the thumbnails are cached, which is what "clear the cache" clears.
    implementation(libs.coil.compose)
}
