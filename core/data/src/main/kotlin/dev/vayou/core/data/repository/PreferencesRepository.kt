package dev.vayou.core.data.repository

import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.PlayerPreferences
import kotlinx.coroutines.flow.StateFlow

/**
 * What the reader has chosen, as it stands and as it changes.
 *
 * A `StateFlow` and not a `Flow`: the theme has to be known before the first frame, and a stream
 * that has not emitted yet would mean a frame drawn in the wrong one.
 */
interface PreferencesRepository {

    val applicationPreferences: StateFlow<ApplicationPreferences>

    suspend fun updateApplicationPreferences(transform: suspend (ApplicationPreferences) -> ApplicationPreferences)

    val playerPreferences: StateFlow<PlayerPreferences>

    suspend fun updatePlayerPreferences(transform: suspend (PlayerPreferences) -> PlayerPreferences)
}
