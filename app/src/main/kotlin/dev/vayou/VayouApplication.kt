package dev.vayou

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hands Coil the loader Hilt built.
 *
 * Without this, `AsyncImage` quietly resolves a default loader of its own -- one that has never
 * heard of [dev.vayou.core.imageloader.VideoThumbnailDecoder] -- and every video tile stays a
 * placeholder with nothing logged to say why.
 */
@HiltAndroidApp
class VayouApplication :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
