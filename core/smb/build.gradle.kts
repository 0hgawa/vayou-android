plugins {
    id("vayou.android.library")
    id("vayou.hilt")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "dev.vayou.core.smb"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    api(libs.androidx.media3.common)
    // BaseDataSource, so ExoPlayer can read a file over SMB the same way it reads one on disk.
    implementation(libs.androidx.media3.exoplayer)

    implementation(libs.smbj)
    // The RPC that asks a server what it shares. SMB2 itself has no call for it.
    implementation(libs.smbjRpc)

    implementation(libs.androidx.datastore.preferences)
}
