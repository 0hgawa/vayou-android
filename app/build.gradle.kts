import java.util.Properties

plugins {
    id("vayou.android.application")
    id("vayou.android.compose")
    id("vayou.hilt")
}

/**
 * The upload key, read from a file the repository does not carry.
 *
 * Absent on every machine but the one that publishes, and the build has to work on the others: with
 * no file the release simply comes out unsigned, which is what a build server or a fresh clone
 * wants anyway.
 *
 * `keystore.properties` sits beside the settings file, and the four names are the ones the old
 * project already uses -- the same file works for both, which matters because they are signed by
 * the same upload key and always will be.
 */
val uploadKeyFile = rootProject.file("keystore.properties")
val uploadKey = Properties().apply {
    if (uploadKeyFile.exists()) uploadKeyFile.inputStream().use(::load)
}

android {
    namespace = "dev.vayou"

    defaultConfig {
        applicationId = "dev.vayou"
        // Carried over, not restarted: the listing was at 58, and a rewrite that ships as version
        // 1 is a different app to every phone that already has this one.
        versionCode = 59
        // Not 0.1.2: what changed between these two is the whole of the app under the screens.
        versionName = "0.2.0"
    }

    signingConfigs {
        if (uploadKeyFile.exists()) {
            create("upload") {
                storeFile = file(uploadKey.getProperty("RELEASE_STORE_FILE"))
                storePassword = uploadKey.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = uploadKey.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = uploadKey.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Null when there is no key file, which leaves the bundle unsigned rather than failing
            // the build for everyone who is not publishing it.
            signingConfig = signingConfigs.findByName("upload")
        }

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
