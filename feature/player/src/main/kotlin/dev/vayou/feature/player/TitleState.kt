package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun rememberTitleState(player: Player): TitleState {
    val titleState = remember { TitleState(player) }
    LaunchedEffect(player) { titleState.observe() }
    return titleState
}

/**
 * What the file playing is called.
 *
 * Asked of the player rather than carried down from whoever opened the screen, because the player
 * is the one that knows: it merges the name the library handed it with whatever the file turns out
 * to carry, and it does that after the first frame, not before.
 */
@UnstableApi
class TitleState(private val player: Player) {
    var title: String? by mutableStateOf(null)
        private set

    suspend fun observe() {
        title = player.mediaMetadata.title?.toString()
        player.listen { events ->
            if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
                title = player.mediaMetadata.title?.toString()
            }
        }
    }
}
