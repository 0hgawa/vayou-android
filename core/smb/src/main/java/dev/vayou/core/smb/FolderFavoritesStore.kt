package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.folderFavoritesDataStore: DataStore<Preferences> by preferencesDataStore("folder_favorites")
private val FOLDER_FAVORITES_KEY = stringPreferencesKey("folder_favorites")

@Singleton
class FolderFavoritesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val favoritesFlow: Flow<List<FavoriteFolder>> = context.folderFavoritesDataStore.data.map { prefs ->
        decode(prefs[FOLDER_FAVORITES_KEY])
    }

    suspend fun toggle(folder: FavoriteFolder) = mutate { current ->
        if (current.any { it.matches(folder.host, folder.share, folder.path) }) {
            current.filterNot { it.matches(folder.host, folder.share, folder.path) }
        } else {
            current + folder
        }
    }

    suspend fun remove(host: String, share: String, path: String) = mutate { current ->
        current.filterNot { it.matches(host, share, path) }
    }

    suspend fun rename(host: String, share: String, path: String, newName: String) = mutate { current ->
        current.map { if (it.matches(host, share, path)) it.copy(displayName = newName) else it }
    }

    private suspend fun mutate(transform: (List<FavoriteFolder>) -> List<FavoriteFolder>) {
        context.folderFavoritesDataStore.edit { prefs ->
            prefs[FOLDER_FAVORITES_KEY] = smbJson.encodeToString(transform(decode(prefs[FOLDER_FAVORITES_KEY])))
        }
    }

    private fun decode(raw: String?): List<FavoriteFolder> {
        raw ?: return emptyList()
        return runCatching { smbJson.decodeFromString<List<FavoriteFolder>>(raw) }.getOrDefault(emptyList())
    }

    private fun FavoriteFolder.matches(host: String, share: String, path: String) =
        this.host == host && this.share == share && this.path == path
}
