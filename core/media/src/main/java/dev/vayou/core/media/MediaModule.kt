package dev.vayou.core.media

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.crossfade
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.media.services.LocalMediaService
import dev.vayou.core.media.services.MediaService
import dev.vayou.core.media.sync.LocalMediaInfoSynchronizer
import dev.vayou.core.media.sync.LocalMediaSynchronizer
import dev.vayou.core.media.sync.MediaInfoSynchronizer
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.ThumbnailGenerationStrategy
import javax.inject.Singleton
import okio.FileSystem

@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {

    @Binds
    @Singleton
    fun bindsMediaSynchronizer(
        mediaSynchronizer: LocalMediaSynchronizer,
    ): MediaSynchronizer

    @Binds
    @Singleton
    fun bindsMediaInfoSynchronizer(
        mediaInfoSynchronizer: LocalMediaInfoSynchronizer,
    ): MediaInfoSynchronizer

    @Binds
    @Singleton
    fun bindMediaService(
        mediaService: LocalMediaService,
    ): MediaService
}
