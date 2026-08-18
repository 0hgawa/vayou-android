plugins {
    id("vayou.android.library")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.core.database"

    // Schemas are checked in so a migration can be tested against the shape it migrates from.
    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
