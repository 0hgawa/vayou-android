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
        // every phone that already has this one. 58 is what the store hands out, and 59, 60 and 61
        // are gone: the store spends a number when a bundle is uploaded, not when one is released,
        // and discarding the draft does not hand it back. 61 went into review and was replaced
        // before it was let out, which spends it the same way.
        versionCode = 62
        // Still 0.2.0, because nobody has seen one yet: 61 carried this name into review and was
        // withdrawn there, so the first 0.2.0 anyone installs is this one. A number the public
        // never saw is not a number to move past.
        versionName = "0.2.0"
    }

    buildTypes {
        /**
         * The release build, signed with the debug key so it can be installed on a phone without
         * the release keystore.
         *
         * It exists to be compared against: a debug build has no R8 and is `debuggable`, and in
         * Compose that is several times slower to open a sheet. Measuring one against the other
         * release measures the two apps rather than the two build types.
         *
         * The same package as the debug build, so it replaces it rather than becoming a third
         * icon -- and so it never collides with a copy installed from the store, which carries a
         * different signature and would refuse it.
         */
        create("comparable") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
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
