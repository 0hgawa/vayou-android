plugins {
    id("vayou.android.library")
    id("vayou.hilt")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "dev.vayou.core.data"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    api(projects.core.model)

    // OpenSubtitles answers in JSON, over plain HTTP. No client library earns its size here.
    implementation(libs.kotlinx.serialization.json)
}
