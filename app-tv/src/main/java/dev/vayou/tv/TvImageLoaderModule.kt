package dev.vayou.tv

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.imageloader.ThumbnailStrategy
import dev.vayou.core.imageloader.VideoThumbnailDecoder
import okio.FileSystem
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TvImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(VideoThumbnailDecoder.Factory(thumbnailStrategy = { ThumbnailStrategy.Hybrid() }))
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(
                DiskCache.Builder()
                    .fileSystem(FileSystem.SYSTEM)
                    .directory(context.filesDir.resolve("thumbnails"))
                    .maxSizePercent(1.0)
                    .build(),
            )
            .crossfade(true)
            .build()
}
