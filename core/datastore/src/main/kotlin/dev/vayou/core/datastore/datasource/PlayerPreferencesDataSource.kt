package dev.vayou.core.datastore.datasource

import androidx.datastore.core.DataStore
import dev.vayou.core.common.Logger
import dev.vayou.core.model.PlayerPreferences
import javax.inject.Inject

class PlayerPreferencesDataSource @Inject constructor(private val store: DataStore<PlayerPreferences>) :
    PreferencesDataSource<PlayerPreferences> {

    override val preferences = store.data

    override suspend fun update(transform: suspend (PlayerPreferences) -> PlayerPreferences) {
        try {
            store.updateData(transform)
        } catch (exception: Exception) {
            Logger.logError(TAG, "Failed to update player preferences: $exception")
        }
    }
}

private const val TAG = "PlayerPreferencesDataSource"
