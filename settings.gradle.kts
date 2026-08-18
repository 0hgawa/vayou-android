pluginManagement {
    // The convention plugins are a build of their own, included before anything else so the
    // modules below can apply them by id.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// `projects.core.ui` instead of `project(":core:ui")`: a mistyped path becomes a compile
// error here rather than a configuration failure at build time.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Vayou"

include(":app")
include(":app-tv")
include(":core:common")
include(":core:database")
include(":core:data")
include(":core:datastore")
include(":core:domain")
include(":core:imageloader")
include(":core:media")
include(":core:model")
include(":core:player")
include(":core:smb")
include(":core:ui")
include(":feature:library")
include(":feature:music")
include(":feature:network")
include(":feature:settings")
include(":feature:player")
