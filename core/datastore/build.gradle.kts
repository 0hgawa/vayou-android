plugins {
    id("vayou.android.library")
    id("vayou.hilt")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "dev.vayou.core.datastore"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)

    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.serialization.json)
}
