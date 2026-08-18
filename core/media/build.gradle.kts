plugins {
    id("vayou.android.library")
    id("vayou.hilt")
    // A queue of tracks travels to the player as one list rather than as parallel arrays.
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "dev.vayou.core.media"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.model)

    implementation(libs.androidx.core.ktx)
    // The synchronizer warms a thumbnail as it records a file, so the library has frames to draw
    // the first time it is opened rather than after a scroll.
    implementation(libs.coil.compose)

    implementation(libs.media.info)

    // Reads and writes ID3 in place. MediaStore holds its own copy of a track's tags, but that copy
    // is rebuilt from the file on every scan -- correcting a name has to happen in the file itself
    // or it comes back wrong a day later.
    implementation(libs.mp3agic)
}
