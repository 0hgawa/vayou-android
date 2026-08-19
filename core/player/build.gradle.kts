plugins {
    id("vayou.android.library")
    id("vayou.android.compose")
    id("vayou.hilt")
}

android {
    namespace = "dev.vayou.core.player"
}

dependencies {
    implementation(projects.core.common)
    // What the equalizer was left set to, which the service restores whenever the audio session
    // is rebuilt -- and the session is rebuilt without anyone asking.
    implementation(projects.core.data)
    // Where a track comes from. Song is in the signature of the queue helpers here, so it travels
    // with them rather than every caller having to have found it for itself.
    api(projects.core.media)
    implementation(projects.core.model)
    // Reading a file off a share, which ExoPlayer does through a DataSource like any other.
    implementation(projects.core.smb)

    implementation(libs.coil.compose)
    implementation(libs.androidx.media3.common)
    // The subtitle renderer and the cues behind it: one of the few Android views that earns its
    // keep, and wanted by both the phone and the television.
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    api(libs.androidx.media3.exoplayer)
    // Without it every m3u8 fails at the source factory with a ClassNotFoundException, which is
    // every IPTV channel in the app: the format the lists are written in is the one format the
    // player could not open.
    implementation(libs.androidx.media3.exoplayer.hls)
    // The session is what the notification, the headset buttons and the lock screen talk to.
    api(libs.androidx.media3.session)
    // await() on the ListenableFuture a session command answers with.
    implementation(libs.kotlinx.coroutines.guava)
    // Software decoders for the formats a device's own hardware refuses -- which is the reason this
    // app exists rather than the system player.
    implementation(libs.media3.ffmpeg.ext)

    // Casting. The framework is Google's; media3-cast is the Player that speaks to it, so the rest
    // of the app drives a Chromecast through the same interface it drives the phone with.
    api(libs.androidx.media3.cast)
    implementation(libs.google.play.services.cast.framework)
    // A receiver is another machine on the network: it fetches a URL, and nothing on this phone is
    // one. This serves the file so it can.
    implementation(libs.nanohttpd)
}
