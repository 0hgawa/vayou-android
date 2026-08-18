package dev.vayou.core.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.media.sync.LocalMediaInfoSynchronizer
import dev.vayou.core.media.sync.LocalMediaSynchronizer
import dev.vayou.core.media.sync.MediaInfoSynchronizer
import dev.vayou.core.media.sync.MediaSynchronizer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {

    @Binds
    @Singleton
    fun bindsMediaSynchronizer(synchronizer: LocalMediaSynchronizer): MediaSynchronizer

    @Binds
    @Singleton
    fun bindsMediaInfoSynchronizer(synchronizer: LocalMediaInfoSynchronizer): MediaInfoSynchronizer
}
