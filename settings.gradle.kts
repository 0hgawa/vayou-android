pluginManagement {
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
rootProject.name = "Vayou"

// App shells
include(":app")
include(":app-tv")

// Core (shared)
include(":core:common")
include(":core:data")
include(":core:smb")
include(":core:database")
include(":core:datastore")
include(":core:domain")
include(":core:imageloader")
include(":core:media")
include(":core:model")
include(":core:player")
include(":core:ui")

// Mobile
include(":mobile:feature-player")
include(":mobile:feature-settings")
include(":mobile:feature-videopicker")

// TV
include(":tv:feature-player")
