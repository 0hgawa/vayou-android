package dev.vayou.feature.player

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouMiniPlayer
import dev.vayou.core.ui.theme.VayouTheme
import kotlinx.coroutines.delay

/**
 * What is on the television, from anywhere else in the app.
 *
 * A film sent to a room does not stop being watched when the player is left, and without this the
 * only way back to the transport is to find the file again and open it. The music player has had
 * this bar since the beginning; casting is the same situation with the speakers further away.
 *
 * Shown while a television is selected *and* the session has a film in it. A route on its own is
 * not enough: picking a television and then browsing without opening anything would put a bar at
 * the foot of the library with nothing behind it. What this reports is a film left running
 * somewhere else, which is exactly the state of having minimised the player.
 */
@Composable
fun CastMiniController(modifier: Modifier = Modifier, onExpand: () -> Unit) {
    val route = rememberSelectedRoute()
    // Only once a television is selected. This bar can show nothing without one, and asking for the
    // controller is what starts the playback service.
    val controller = rememberPlaybackController(isWanted = route != null)

    var hasFilm by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(controller, route) {
        val player = controller ?: return@LaunchedEffect
        // Polled rather than listened to: the position moves without an event, and this bar is a
        // progress line and a play button -- everything on it changes with the clock.
        while (route != null) {
            // A film and not merely something in the session. One session carries both, and the
            // music bar is already showing whatever song is in it -- asking only whether anything
            // was queued put the two bars on the screen at once, one under the other, both about
            // the same track.
            val item = player.currentMediaItem
            hasFilm = item != null && item.mediaMetadata.mediaType != MediaMetadata.MEDIA_TYPE_MUSIC
            isPlaying = player.isPlaying
            title = item?.mediaMetadata?.title?.toString().orEmpty()
            progress = (player.currentPosition.toFloat() / player.duration.coerceAtLeast(1)).coerceIn(0f, 1f)
            delay(TickMs)
        }
    }

    val deviceName = route?.name ?: stringResource(R.string.cast)

    VayouMiniPlayer(
        visible = route != null && hasFilm,
        // The room is the subject when the film has no name to show: a bar reading "Cast" over
        // "Cast" says nothing twice.
        title = title.ifEmpty { deviceName },
        subtitle = if (title.isEmpty()) "" else deviceName,
        progress = progress,
        onClick = onExpand,
        modifier = modifier,
        leading = {
            VayouArtwork(
                model = controller?.currentMediaItem?.mediaId,
                iconTint = VayouTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(ArtworkSize),
                icon = VayouIcons.Cast,
                shape = VayouTheme.shapes.small,
            )
        },
        actions = {
            VayouIconButton(
                onClick = {
                    val player = controller ?: return@VayouIconButton
                    if (player.isPlaying) player.pause() else player.play()
                },
            ) {
                Icon(
                    imageVector = if (isPlaying) VayouIcons.PauseFilled else VayouIcons.Play,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    tint = VayouTheme.colors.onSurface,
                )
            }
        },
    )
}

/** Twice a second, which is what a progress line under a title needs and no more. */
private const val TickMs = 500L

/** The same square the music bar leads with, so the two stack as one object. */
private val ArtworkSize = 40.dp
