import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal val Project.libs
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.version(alias: String): String = libs.findVersion(alias).get().toString()

/**
 * The half of an Android module's setup that is the same whether it is an app or a library:
 * which SDK it compiles against, which it runs on, and which JVM its bytecode targets.
 *
 * Stated once here rather than in each module, which is what let them drift apart before.
 *
 * Written through the getters rather than the `compileOptions { }` blocks: AGP 9 keeps the block
 * form on the concrete extensions only, and taking [CommonExtension] is what lets an app and a
 * library share this.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    val jvm = JavaVersion.toVersion(version("android-jvm"))

    extension.compileSdk = version("android-compileSdk").toInt()
    extension.defaultConfig.minSdk = version("android-minSdk").toInt()

    extension.compileOptions.sourceCompatibility = jvm
    extension.compileOptions.targetCompatibility = jvm

    // Two licence copies every Kotlin coroutines artifact carries. Packaged twice, they collide.
    extension.packaging.resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")

    extensions.getByType<KotlinAndroidProjectExtension>().compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jvm.toString()))
    }
}
