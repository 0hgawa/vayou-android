plugins {
    id("vayou.android.library")
    id("vayou.android.compose")
}

android {
    namespace = "dev.vayou.core.ui"
}

dependencies {
    // The layout defaults speak in terms of a layout mode, and a thumbnail is asked for by uri.
    implementation(projects.core.model)
    implementation(libs.coil.compose)

    // For ColorUtils, which is where the RGB-to-HSL the dominant colour needs actually lives.
    implementation(libs.androidx.core.ktx)

    // The one module allowed to depend on Material 3. Everything else takes its components from
    // here, so an M3 import anywhere else is a dependency someone had to add on purpose.
    api(libs.androidx.compose.material3)
}
