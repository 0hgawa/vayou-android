package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import dev.vayou.core.player.setSkipSilence
import dev.vayou.core.player.skipSilence

/**
 * Whether the quiet stretches are run together rather than played.
 *
 * Worth the most on a lecture or a podcast, where the pauses are half the running time; worth
 * nothing on a film, where they are the pacing. That is why it sits beside the playback speed
 * instead of in the settings: it is set for one thing being listened to, not once for everything.
 *
 * The value belongs to the player in the service, so a screen reopened over a running lecture has
 * to be told what it is rather than assume it is off. Asked once, and owned here from then on --
 * nothing else in the app changes it, so reading it back on every open would be asking for our own
 * answer.
 */
@Stable
class SkipSilenceState(private val controller: MediaController) {

    var isEnabled: Boolean by mutableStateOf(false)
        private set

    suspend fun refresh() {
        isEnabled = controller.skipSilence()
    }

    fun toggle() {
        isEnabled = !isEnabled
        controller.setSkipSilence(isEnabled)
    }
}

@Composable
fun rememberSkipSilenceState(controller: MediaController): SkipSilenceState {
    val state = remember(controller) { SkipSilenceState(controller) }
    LaunchedEffect(state) { state.refresh() }
    return state
}
