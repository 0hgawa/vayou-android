plugins {
    id("vayou.android.application")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou"

    defaultConfig {
        applicationId = "dev.vayou"
        // Carried over, not restarted: a rewrite that ships as version 1 is a different app to
        // every phone that already has this one. 58 is what the store hands out, and 59 and 60 are
        // gone: the store spends a number when a bundle is uploaded, not when one is released, and
        // discarding the draft does not hand it back.
        versionCode = 61
        // Not 0.1.2: what changed between these two is the whole of the app under the screens.
        versionName = "0.2.0"
    }

    buildTypes {
        debug {
            // `.next`, not the `.debug` the convention plugin gives: that is the package the old
            // build already occupies on a developer's phone, and this one has to sit beside it
            // until it is the better of the two. Drops away when the old project does.
            applicationIdSuffix = ".next"
            versionNameSuffix = "-next"
        }

        /**
         * The release build, signed with the debug key so it can be installed on a phone without
         * the release keystore.
         *
         * It exists to be compared against: a debug build has no R8 and is `debuggable`, and in
         * Compose that is several times slower to open a sheet. Measuring one against the other
         * release measures the two apps rather than the two build types.
         *
         * The same package as the debug build, so it replaces it rather than becoming a third icon.
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
    implementation(projects.core.imageloader)
    implementation(projects.core.model)
    implementation(projects.core.media)
    implementation(projects.core.ui)
    implementation(projects.feature.library)
    implementation(projects.feature.music)
    implementation(projects.feature.network)
    implementation(projects.feature.settings)
    implementation(projects.feature.player)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // Which of the bar and the rail the window is wide enough for.
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.accompanist.permissions)
    implementation(libs.coil.compose)
    // Coil 3 ships no network client of its own: without this on the classpath it fetches nothing
    // over http, and every station logo and remote cover is a blank square.
    implementation(libs.coil.network.okhttp)
}
