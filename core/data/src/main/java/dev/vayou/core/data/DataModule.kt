package dev.vayou.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.data.repository.FavoritesRepository
import dev.vayou.core.data.repository.LocalFavoritesRepository
import dev.vayou.core.data.repository.LocalMediaRepository
import dev.vayou.core.data.repository.LocalPlaylistRepository
import dev.vayou.core.data.repository.LocalPreferencesRepository
import dev.vayou.core.data.repository.LocalPrivateRepository
import dev.vayou.core.data.repository.LocalSearchHistoryRepository
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.OpenSubtitlesRepository
import dev.vayou.core.data.repository.PlaylistRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.data.repository.PrivateRepository
import dev.vayou.core.data.repository.RemoteOpenSubtitlesRepository
import dev.vayou.core.data.repository.SearchHistoryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsMediaRepository(
        videoRepository: LocalMediaRepository,
    ): MediaRepository

    @Binds
    @Singleton
    fun bindsPreferencesRepository(
        preferencesRepository: LocalPreferencesRepository,
    ): PreferencesRepository

    @Binds
    @Singleton
    fun bindsSearchHistoryRepository(
        searchHistoryRepository: LocalSearchHistoryRepository,
    ): SearchHistoryRepository

    @Binds
    @Singleton
    fun bindsOpenSubtitlesRepository(
        repository: RemoteOpenSubtitlesRepository,
    ): OpenSubtitlesRepository

    @Binds
    @Singleton
    fun bindsPlaylistRepository(
        repository: LocalPlaylistRepository,
    ): PlaylistRepository

    @Binds
    @Singleton
    fun bindsFavoritesRepository(
        repository: LocalFavoritesRepository,
    ): FavoritesRepository

    @Binds
    @Singleton
    fun bindsPrivateRepository(
        repository: LocalPrivateRepository,
    ): PrivateRepository
}
