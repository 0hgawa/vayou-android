plugins {
    `kotlin-dsl`
}

group = "dev.vayou.buildlogic"

// Bytecode target rather than a toolchain: a toolchain asks Gradle to go and find a JDK of that
// exact version, and the only JDK here is the one already running the build. Targeting is enough --
// the plugins are loaded by that same daemon.
val jvm = JavaVersion.toVersion(libs.versions.android.jvm.get())

java {
    sourceCompatibility = jvm
    targetCompatibility = jvm
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvm.toString()))
    }
}

dependencies {
    // compileOnly, not implementation: these plugins are on the build classpath already by the
    // time a convention plugin runs. Pulling them in for real would put two copies there.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "vayou.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("jvmLibrary") {
            id = "vayou.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "vayou.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "vayou.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "vayou.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
