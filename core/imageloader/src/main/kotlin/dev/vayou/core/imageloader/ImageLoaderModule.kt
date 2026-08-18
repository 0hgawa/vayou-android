package dev.vayou.core.imageloader

import android.content.Context
import coil3.ImageLoader
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.ThumbnailGenerationStrategy
import javax.inject.Singleton

/**
 * The one image loader, for whichever shell is running.
 *
 * Here rather than in an application module because both of them want the same one: a television
 * shows the same thumbnails off the same files, and two copies of this would be two places to
 * remember when a decoder is added.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        preferencesRepository: PreferencesRepository,
    ): ImageLoader = ImageLoader.Builder(context)
        .components {
            // A frame out of the file itself, since a video has no cover to fetch. Which frame is
            // read at decode time rather than captured here, so changing the preference does not
            // need the loader rebuilt.
            add(
                VideoThumbnailDecoder.Factory(
                    thumbnailStrategy = {
                        preferencesRepository.applicationPreferences.value.thumbnailStrategy
                    },
                ),
            )
        }
        // The frame arrives long after the row does, and cutting to it is a flash.
        .crossfade(true)
        .build()
}

private val ApplicationPreferences.thumbnailStrategy: ThumbnailStrategy
    get() = when (thumbnailGenerationStrategy) {
        ThumbnailGenerationStrategy.FIRST_FRAME -> ThumbnailStrategy.FirstFrame
        ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE ->
            ThumbnailStrategy.FrameAtPercentage(thumbnailFramePosition)
        // Takes the frame at the position, and falls back to the first one if it comes out a flat
        // colour -- which is what a fade-in at 33% of a film looks like.
        ThumbnailGenerationStrategy.HYBRID -> ThumbnailStrategy.Hybrid(thumbnailFramePosition)
    }
