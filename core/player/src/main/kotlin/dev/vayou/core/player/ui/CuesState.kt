package dev.vayou.core.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun rememberCuesState(player: Player): CuesState {
    val cuesState = remember { CuesState(player) }
    LaunchedEffect(player) { cuesState.observe() }
    return cuesState
}

/** The lines the player wants shown right now, and nothing about how they look. */
@UnstableApi
class CuesState(private val player: Player) {
    var cues: List<Cue> by mutableStateOf(emptyList())
        private set

    suspend fun observe() {
        cues = player.currentCues.cues
        player.listen { events ->
            if (events.contains(Player.EVENT_CUES)) {
                cues = player.currentCues.cues
            }
        }
    }
}
