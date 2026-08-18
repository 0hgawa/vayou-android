import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Compose, and the bill of materials that keeps its artifacts on one version.
 *
 * Applied on top of the library or application plugin rather than folded into them: not every
 * module draws, and the ones that do not should not pay for the compiler plugin.
 *
 * Foundation and no further. Material 3 belongs to `core:ui` alone, so that a screen reaching for
 * an M3 component instead of the app's own has to add the dependency to say so.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.getByType<CommonExtension>().buildFeatures.compose = true

        dependencies {
            val bom = platform(libs.findLibrary("androidx-compose-bom").get())
            add("implementation", bom)
            add("androidTestImplementation", bom)
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-foundation").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }

        extensions.configure<ComposeCompilerGradlePluginExtension> {
            // Written on request rather than always: the stability reports are read when something
            // recomposes that should not, and generating them every build costs every build.
            if (providers.gradleProperty("vayou.composeReports").isPresent) {
                val dir = layout.buildDirectory.dir("compose-reports")
                reportsDestination.set(dir)
                metricsDestination.set(dir)
            }
        }
    }
}
