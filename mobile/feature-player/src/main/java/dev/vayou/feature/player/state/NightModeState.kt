package dev.vayou.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.player.service.setNightModeEnabled

@Stable
class NightModeState(
    private val controller: MediaController,
    initialEnabled: Boolean,
    private val onSave: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
) {
    var isEnabled by mutableStateOf(initialEnabled)
        private set

    fun toggle() {
        val newValue = !isEnabled
        isEnabled = newValue
        controller.setNightModeEnabled(newValue)
        onSave { it.copy(nightModeEnabled = newValue) }
    }
}

@Composable
fun rememberNightModeState(
    controller: MediaController?,
    preferences: PlayerPreferences,
    onSave: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
): NightModeState? {
    val ctrl = controller ?: return null
    return remember(ctrl) {
        NightModeState(
            controller = ctrl,
            initialEnabled = preferences.nightModeEnabled,
            onSave = onSave,
        )
    }
}
