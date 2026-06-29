package dev.vayou.playlist

import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.MediaLayoutMode

internal suspend fun PreferencesRepository.toggleMediaLayoutMode() {
    updateApplicationPreferences { prefs ->
        prefs.copy(
            mediaLayoutMode = if (prefs.mediaLayoutMode == MediaLayoutMode.GRID) {
                MediaLayoutMode.LIST
            } else {
                MediaLayoutMode.GRID
            },
        )
    }
}
