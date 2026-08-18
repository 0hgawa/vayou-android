plugins {
    id("vayou.android.library")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.core.domain"
}

dependencies {
    implementation(projects.core.common)
    api(projects.core.data)
    api(projects.core.model)
}
