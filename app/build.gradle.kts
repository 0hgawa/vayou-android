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
        // every phone that already has this one. 58 is what the store hands out; 59, 60 and 61 are
        // gone, because the store spends a number when a bundle is uploaded and not when one is
        // released, and neither discarding a draft nor withdrawing one from review hands it back.
        // 62 is the first that got out.
        versionCode = 63
        // 0.2.1, because 0.2.0 is now on phones. It went out through the store on 30/08 and was
        // installed the same evening, which is what the note here used to be waiting for: while it
        // existed only inside a review, moving past it would have marked a correction over
        // something nobody had. It is no longer that, so what follows it is a correction.
        versionName = "0.2.1"
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
