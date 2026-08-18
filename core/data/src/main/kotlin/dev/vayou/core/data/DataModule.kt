package dev.vayou.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.data.repository.LocalMediaPlaylistRepository
import dev.vayou.core.data.repository.LocalMediaRepository
import dev.vayou.core.data.repository.LocalPreferencesRepository
import dev.vayou.core.data.repository.LocalSearchHistoryRepository
import dev.vayou.core.data.repository.MediaPlaylistRepository
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.OpenSubtitlesRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.data.repository.RemoteOpenSubtitlesRepository
import dev.vayou.core.data.repository.SearchHistoryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsMediaRepository(repository: LocalMediaRepository): MediaRepository

    @Binds
    @Singleton
    fun bindsPreferencesRepository(repository: LocalPreferencesRepository): PreferencesRepository

    @Binds
    @Singleton
    fun bindsMediaPlaylistRepository(repository: LocalMediaPlaylistRepository): MediaPlaylistRepository

    @Binds
    @Singleton
    fun bindsSearchHistoryRepository(repository: LocalSearchHistoryRepository): SearchHistoryRepository

    @Binds
    @Singleton
    fun bindsOpenSubtitlesRepository(repository: RemoteOpenSubtitlesRepository): OpenSubtitlesRepository
}
