import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The two shells, `app` and `app-tv`. Only what they share -- an id and a version are their own. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        // The upload key, read from a file the repository does not carry. Absent on every machine
        // but the one that publishes, and the build has to work on the others.
        //
        // One key for both shells. They are two listings in the store and always will be -- a
        // listing is a package name, and theirs differ -- but a key covers any number of them, and
        // a second would only be a second thing to lose.
        val keyFile = rootProject.file("keystore.properties")
        val key = Properties().apply { if (keyFile.exists()) keyFile.inputStream().use(::load) }

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig.targetSdk = version("android-targetSdk").toInt()

            if (keyFile.exists()) {
                signingConfigs.create("upload").apply {
                    storeFile = file(key.getProperty("RELEASE_STORE_FILE"))
                    storePassword = key.getProperty("RELEASE_STORE_PASSWORD")
                    keyAlias = key.getProperty("RELEASE_KEY_ALIAS")
                    keyPassword = key.getProperty("RELEASE_KEY_PASSWORD")
                }
            }

            buildTypes {
                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    // Null where there is no key file, which leaves the bundle unsigned rather than
                    // failing the build for everyone who is not publishing it.
                    signingConfig = signingConfigs.findByName("upload")
                }
                debug {
                    applicationIdSuffix = ".debug"
                }
            }

            // Dependency metadata is a signing-time cost and a privacy surface, and nothing here
            // reads it back.
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
        }
    }
}
