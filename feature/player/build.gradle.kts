plugins {
    id("vayou.android.library")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.feature.player"
}

dependencies {
    // Where the film was left is read and written here, so the next open starts where the last
    // one stopped.
    implementation(projects.core.common)
    implementation(projects.core.data)
    // The queue is the folder in the order the library shows it, which is the use case's job.
    implementation(projects.core.domain)
    implementation(projects.core.player)
    implementation(projects.core.ui)

    implementation(libs.reorderable)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.hilt.navigation.compose)
    // Which televisions are on this network, and which one is selected. The framework holds that,
    // so the button reads it rather than keeping a second copy.
    implementation(libs.google.play.services.cast.framework)
}
