plugins {
    id("vayou.android.application")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.tv"

    defaultConfig {
        // Not `dev.vayou.tv`, which was spent on a listing that was deleted. A package name is
        // handed out once and never returned, so the second listing needs a second name -- and
        // this is the whole of what changes: the namespace below, the source packages and every
        // class stay `dev.vayou.tv`, because what a store calls an app and what its code is called
        // are two different things.
        applicationId = "dev.vayoutv"
        // From one, unlike the phone: this is a listing of its own, and nothing has ever been
        // installed from it. The phone carries its old numbering because it has an audience to
        // keep; the television has none yet.
        //
        // Back to one with the new name: the numbers spent under the old one went with it, and a
        // listing that has never handed out a build starts where every listing starts.
        versionCode = 1
        // Below the phone's 0.2.0, because that is where this shell actually stands. 1.0 is the
        // number that says a thing is finished, and the television is the younger of the two: the
        // phone has an audience and more of the rewrite behind it. A number that claims more than
        // the app has is a promise somebody else has to keep.
        //
        // Its own line, not the phone's. They share every core module but ship as two listings,
        // and a viewer of one never sees the other's number.
        versionName = "0.1.0"
    }

    /**
     * Every language in every install, rather than only the ones the set is configured in.
     *
     * The television picks its language in this app's own settings, because Android TV offers no
     * system screen that does it. Play, left to itself, delivers only the languages a device is
     * already set to -- so a set bought in one country would be offered ten languages and handed
     * one, and the choice would do nothing, silently. That is the whole reason the setting exists.
     *
     * It costs about forty kilobytes a language in an app of sixty megabytes.
     */
    bundle {
        language {
            enableSplit = false
        }
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
