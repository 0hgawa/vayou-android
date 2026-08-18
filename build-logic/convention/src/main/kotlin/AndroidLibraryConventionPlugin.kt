import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Every `core:*` and `feature:*` module. Applying this is the whole of an Android library's setup.
 *
 * No `targetSdk`: a library does not declare one. What it runs against is decided by the shell
 * that packages it.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)

            // A library has no release build of its own to shrink; the app it lands in does that,
            // and leaving it on here only slows every library build down for nothing.
            buildTypes.configureEach { isMinifyEnabled = false }
        }
    }
}
