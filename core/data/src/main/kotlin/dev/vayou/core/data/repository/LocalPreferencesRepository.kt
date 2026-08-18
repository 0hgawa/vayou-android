package dev.vayou.core.data.repository

import dev.vayou.core.common.di.ApplicationScope
import dev.vayou.core.datastore.datasource.AppPreferencesDataSource
import dev.vayou.core.datastore.datasource.PlayerPreferencesDataSource
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LocalPreferencesRepository @Inject constructor(
    appPreferencesDataSource: AppPreferencesDataSource,
    private val playerPreferencesDataSource: PlayerPreferencesDataSource,
    @ApplicationScope applicationScope: CoroutineScope,
) : PreferencesRepository {

    private val dataSource = appPreferencesDataSource

    // Eagerly, because the theme reads this before the first frame and a lazy start would draw one
    // frame in the wrong theme before the real value arrived.
    override val applicationPreferences: StateFlow<ApplicationPreferences> =
        dataSource.preferences.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = ApplicationPreferences(),
        )

    override suspend fun updateApplicationPreferences(
        transform: suspend (ApplicationPreferences) -> ApplicationPreferences,
    ) {
        dataSource.update(transform)
    }

    // Lazily, unlike the theme above: nothing draws a frame waiting on how a subtitle should look,
    // and a store opened for a viewer who never opens the player is a file read for nothing.
    override val playerPreferences: StateFlow<PlayerPreferences> =
        playerPreferencesDataSource.preferences.stateIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
            initialValue = PlayerPreferences(),
        )

    override suspend fun updatePlayerPreferences(transform: suspend (PlayerPreferences) -> PlayerPreferences) {
        playerPreferencesDataSource.update(transform)
    }
}

private const val StopTimeoutMillis = 5_000L
