package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.channelFavouritesDataStore: DataStore<Preferences> by preferencesDataStore("favorites")

private val OnlyStarredKey = booleanPreferencesKey("only_favourite_channels")

/** The live channels the viewer starred, in the order they were starred. */
@Singleton
class ChannelFavouritesStore @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.channelFavouritesDataStore

    private val store = JsonListStore(dataStore, "favorite_channels", PlaylistChannel.serializer())

    val favourites: Flow<List<PlaylistChannel>> = store.flow

    /**
     * Whether the starred are all the viewer is looking at.
     *
     * Kept on disc for the same reason the chosen country is: it is not a switch flicked in passing
     * but an answer to "what am I looking at", and a viewer who left a list of twelve channels and
     * came back to a thousand would have to give the answer again every evening.
     */
    val isOnlyStarred: Flow<Boolean> = dataStore.data.map { it[OnlyStarredKey] ?: false }

    suspend fun setOnlyStarred(isOnly: Boolean) {
        dataStore.edit { it[OnlyStarredKey] = isOnly }
    }

    /** A set, because every row asks "is this one starred" and none asks about the order. */
    val favouriteUrls: Flow<Set<String>> = favourites.map { list ->
        list.mapTo(mutableSetOf(), PlaylistChannel::url)
    }

    suspend fun toggle(channel: PlaylistChannel) = store.update { current ->
        if (current.any { it.url == channel.url }) {
            current.filterNot { it.url == channel.url }
        } else {
            current + channel
        }
    }

    /**
     * Star or unstar a whole selection, in one edit.
     *
     * Toggling each in turn would be one datastore write per channel and would leave a mixed
     * selection alternating rather than agreeing -- the point of picking several is that they end
     * up the same.
     */
    suspend fun setFavourite(channels: List<PlaylistChannel>, isFavourite: Boolean) = store.update { current ->
        val urls = channels.mapTo(mutableSetOf(), PlaylistChannel::url)
        if (isFavourite) {
            current + channels.filterNot { channel -> current.any { it.url == channel.url } }
        } else {
            current.filterNot { it.url in urls }
        }
    }

    suspend fun rename(url: String, newName: String) = store.update { current ->
        current.map { if (it.url == url) it.copy(name = newName) else it }
    }
}
