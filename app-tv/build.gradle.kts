plugins {
    id("vayou.android.application")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.tv"

    defaultConfig {
        applicationId = "dev.vayou.tv"
        // From one, unlike the phone: this is a listing of its own, and nothing has ever been
        // installed from it. The phone carries its old numbering because it has an audience to
        // keep; the television has none yet.
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            // Beside the old TV build rather than over it, for the reason the phone's shell gives.
            applicationIdSuffix = ".next"
            versionNameSuffix = "-next"
        }

        /**
         * The build to judge this app by, as the phone has one.
         *
         * A television is the machine where the difference tells most: the phone opens four times
         * faster once R8 has been over it, and the set has a fraction of the phone's processor.
         * Measured on the debug build, a screen here drops nearly every frame -- which says nothing
         * about the app and everything about what `debuggable` costs.
         *
         * Signed with the debug key so it installs without the upload one, and carrying the same
         * `.next` package so it replaces the debug build rather than becoming a third icon.
         */
        create("comparable") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".next"
            versionNameSuffix = "-next"
            matchingFallbacks.add("release")
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.imageloader)
    implementation(projects.core.media)
    implementation(projects.core.model)
    implementation(projects.core.player)
    implementation(projects.core.smb)
    implementation(projects.core.ui)
    // The phone's settings store and its names for every one of them. A television that kept its
    // own copy would be a second place for "reset" to mean something slightly different.
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.coil.compose)
    // As on the phone: Coil 3 fetches nothing over http without a client on the classpath, and a
    // channel list is all remote logos.
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.kotlinx.coroutines.guava)

    // The set built for a remote control: focus that is visible from three metres, and rows that
    // scroll under a D-pad rather than a thumb.
    implementation(libs.androidx.tv.material)
}
