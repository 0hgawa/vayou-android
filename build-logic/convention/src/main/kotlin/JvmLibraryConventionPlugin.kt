import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * A module with no Android in it -- the types the app is about, and nothing that needs a device to
 * compile.
 *
 * Plain Kotlin builds faster than an Android library and, more usefully, the compiler refuses the
 * moment someone reaches for a `Context` in a place that should not know what one is.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        val jvm = JavaVersion.toVersion(version("android-jvm"))

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = jvm
            targetCompatibility = jvm
        }

        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(jvm.toString()))
            }
        }
    }
}
