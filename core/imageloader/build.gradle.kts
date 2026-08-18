plugins {
    id("vayou.android.library")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.core.imageloader"
}

dependencies {
    // The thumbnail strategy is a preference, and the loader is built once from whatever it says.
    implementation(projects.core.data)
    implementation(projects.core.model)

    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    // Decodes a frame out of a container the platform's own retriever cannot open. The two travel
    // together: mediainfo is the wrapper, and the ffmpeg extension carries the native libraries it
    // calls into. Without the second, the first fails silently and every tile stays a placeholder.
    implementation(libs.media.info)
    implementation(libs.media3.ffmpeg.ext)
}
