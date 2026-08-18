plugins {
    id("vayou.android.library")
    id("vayou.android.compose")
    id("vayou.hilt")
    // No version: the Kotlin Android plugin the convention applies already put it on the
    // classpath, and naming a version again asks Gradle to resolve one it cannot check.
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "dev.vayou.feature.music"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    // The session it attaches to, and the equalizer and sleep timer it shares with the video side.
    implementation(projects.core.data)
    // Sharing and deleting a track, which both go through MediaStore.
    implementation(projects.core.media)
    implementation(projects.core.model)
    implementation(projects.core.player)
    implementation(projects.feature.player)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.coil.compose)
    // Dragging a queue row to a new place, which Compose has no primitive for.
    implementation(libs.reorderable)
    implementation(libs.accompanist.permissions)
    // await() on the ListenableFuture the session controller is built through.
    implementation(libs.kotlinx.coroutines.guava)
}
