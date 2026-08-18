import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The two shells, `app` and `app-tv`. Only what they share -- an id and a version are their own. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(this)
            defaultConfig.targetSdk = version("android-targetSdk").toInt()

            buildTypes {
                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
