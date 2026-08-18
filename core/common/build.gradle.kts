plugins {
    id("vayou.android.library")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    // Guesses a subtitle's encoding. Media3 reads them as UTF-8 and offers nowhere to say
    // otherwise, and a Latin-1 .srt is the common case, not the exotic one.
    implementation(libs.github.albfernandez.juniversalchardet)
}
