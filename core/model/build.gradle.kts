plugins {
    id("vayou.jvm.library")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    // Only what the preferences need to reach disk. No Android here: this module is the app's
    // vocabulary, and it should fail to compile if something reaches for a Context.
    implementation(libs.kotlinx.serialization.json)
}
