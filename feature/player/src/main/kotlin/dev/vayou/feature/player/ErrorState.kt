package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun rememberErrorState(player: Player): ErrorState {
    val errorState = remember { ErrorState(player) }
    LaunchedEffect(player) { errorState.observe() }
    return errorState
}

/**
 * Why the film stopped, when it stopped for a reason.
 *
 * Read once on arrival as well as watched for, because a file can fail before anything is listening
 * — a rotation composes this afresh, and the failure it is meant to report is already in the past.
 */
@UnstableApi
class ErrorState(private val player: Player) {
    var error: PlaybackException? by mutableStateOf(null)
        private set

    fun retry() {
        error = null
        player.prepare()
        player.play()
    }

    suspend fun observe() {
        error = player.playerError
        player.listen { events ->
            if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                error = player.playerError
            }
        }
    }
}
