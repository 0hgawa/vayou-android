package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

private val Context.folderFavouritesDataStore: DataStore<Preferences> by preferencesDataStore("folder_favorites")

/** Shares and directories the viewer pinned, so a deep path is one tap from the server list. */
@Singleton
class FolderFavouritesStore @Inject constructor(@ApplicationContext context: Context) {

    private val store =
        JsonListStore(context.folderFavouritesDataStore, "folder_favorites", FavoriteFolder.serializer())

    val favourites: Flow<List<FavoriteFolder>> = store.flow

    suspend fun toggle(folder: FavoriteFolder) = store.update { current ->
        if (current.any { it.isAt(folder.host, folder.share, folder.path) }) {
            current.filterNot { it.isAt(folder.host, folder.share, folder.path) }
        } else {
            current + folder
        }
    }

    suspend fun remove(host: String, share: String, path: String) = store.update { current ->
        current.filterNot { it.isAt(host, share, path) }
    }

    suspend fun rename(host: String, share: String, path: String, newName: String) = store.update { current ->
        current.map { if (it.isAt(host, share, path)) it.copy(displayName = newName) else it }
    }
}

/** Identity is the whole address: the same folder name under two shares is two folders. */
private fun FavoriteFolder.isAt(host: String, share: String, path: String) =
    this.host == host && this.share == share && this.path == path
