package dev.vayou.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.common.Dispatcher
import dev.vayou.core.common.VayouDispatchers
import dev.vayou.core.common.di.ApplicationScope
import dev.vayou.core.datastore.serializer.ApplicationPreferencesSerializer
import dev.vayou.core.datastore.serializer.MediaPlaylistsSerializer
import dev.vayou.core.datastore.serializer.PlayerPreferencesSerializer
import dev.vayou.core.datastore.serializer.SearchHistorySerializer
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.MediaPlaylists
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.SearchHistory
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * The file name is the contract: an installed copy already has one of these on disk, and renaming
 * it loses every preference the reader ever set.
 */
private const val APP_PREFERENCES_FILE = "app_preferences.json"
private const val PLAYER_PREFERENCES_FILE = "player_preferences.json"
private const val MEDIA_PLAYLISTS_FILE = "media_playlists.json"
private const val SEARCH_HISTORY_FILE = "search_history.json"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideAppPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(VayouDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<ApplicationPreferences> = DataStoreFactory.create(
        serializer = ApplicationPreferencesSerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        produceFile = { context.dataStoreFile(APP_PREFERENCES_FILE) },
    )

    @Provides
    @Singleton
    fun providePlayerPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(VayouDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<PlayerPreferences> = DataStoreFactory.create(
        serializer = PlayerPreferencesSerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        produceFile = { context.dataStoreFile(PLAYER_PREFERENCES_FILE) },
    )

    @Provides
    @Singleton
    fun provideMediaPlaylistsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(VayouDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<MediaPlaylists> = DataStoreFactory.create(
        serializer = MediaPlaylistsSerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        produceFile = { context.dataStoreFile(MEDIA_PLAYLISTS_FILE) },
    )

    @Provides
    @Singleton
    fun provideSearchHistoryDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(VayouDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<SearchHistory> = DataStoreFactory.create(
        serializer = SearchHistorySerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        produceFile = { context.dataStoreFile(SEARCH_HISTORY_FILE) },
    )
}
