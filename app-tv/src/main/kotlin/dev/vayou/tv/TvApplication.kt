package dev.vayou.tv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The television's shell.
 *
 * The same shape as the phone's and a separate application all the same: the two are different
 * packages on different devices, and everything they have in common is a module below them both.
 */
@HiltAndroidApp
class TvApplication :
    Application(),
    SingletonImageLoader.Factory {

    /**
     * The one loader, built by the graph so that its decoders and its cache are configured in a
     * single place rather than at each call.
     */
    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
