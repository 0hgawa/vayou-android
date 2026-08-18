plugins {
    id("vayou.android.library")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.feature.library"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    // Moving a film into the app’s own storage, which is what the locked folder is.
    implementation(projects.core.media)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    // The cast key, which is the same key on every bar in the app.
    implementation(projects.feature.player)

    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.hilt.navigation.compose)
    // The phone’s own lock, asked for before the private folder opens.
    implementation(libs.androidx.biometric)
}
