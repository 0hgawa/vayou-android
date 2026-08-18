package dev.vayou.feature.music

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import dev.vayou.core.player.ui.rememberMusicController
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouMiniPlayer
import dev.vayou.core.ui.theme.VayouTheme
import kotlinx.coroutines.delay

/**
 * What is playing, kept above the navigation bar while the library is on screen.
 *
 * It connects to the shared session only while its host screen is started, so it never wakes the
 * engine at launch, and it only appears for a session that is actually playing something.
 */
@Composable
fun MusicMiniController(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isQueueOpen by remember { mutableStateOf(false) }

    val controller = rememberMusicController { player ->
        currentItem = player.currentMediaItem
        isPlaying = player.isPlaying
        playbackState = player.playbackState
    }

    LaunchedEffect(controller, isPlaying) {
        val active = controller
        while (active != null && isPlaying) {
            progress = (active.currentPosition.toFloat() / active.duration.coerceAtLeast(1)).coerceIn(0f, 1f)
            delay(TickMs)
        }
    }

    val item = currentItem
    // The item's own metadata, not the player's: the player merges in whatever the file's tags say,
    // and this has to be the label the app put on when it queued the thing.
    val isMusic = item?.mediaMetadata?.mediaType == MediaMetadata.MEDIA_TYPE_MUSIC
    val metadata = controller?.mediaMetadata
    val title = metadata?.title?.toString()?.takeIf { it.isNotBlank() }
        ?: item?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
        ?: item?.localConfiguration?.uri?.lastPathSegment
        ?: ""
    val artist = metadata?.artist?.toString()?.takeIf { it.isNotBlank() }
        ?: item?.mediaMetadata?.artist?.toString()?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.unknown_artist)

    VayouMiniPlayer(
        // Deliberately not gated on the controller. It is released whenever the host screen stops
        // and reconnects asynchronously on start, so gating on it would slide the bar out and back
        // in on every app switch. What is playing does not change while we are away.
        // One session carries both a film and a song -- the notification, the headset buttons and
        // the lock screen are one set of controls, and two sessions would mean two of each. So this
        // bar asks what is in it: a film playing on in the background is not something to offer
        // here, and tapping it would open the audio player on a video.
        visible = item != null && isMusic && playbackState in ActiveStates,
        title = title,
        subtitle = artist,
        progress = progress,
        onClick = {
            item?.mediaId?.let { context.startActivity(MusicPlayerActivity.intentFor(context, it)) }
        },
        modifier = modifier,
        leading = {
            // The same stack the list rows use: the note is the floor, and the cover lays over it
            // when there is one and when it actually resolves.
            VayouArtwork(
                model = metadata?.artworkUri ?: item?.mediaMetadata?.artworkUri,
                iconTint = VayouTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(MiniArtworkSize),
                shape = VayouTheme.shapes.small,
            )
        },
        actions = {
            VayouIconButton(onClick = { controller?.let { if (isPlaying) it.pause() else it.play() } }) {
                Icon(
                    imageVector = if (isPlaying) VayouIcons.PauseFilled else VayouIcons.Play,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    tint = VayouTheme.colors.onSurface,
                )
            }
            // Skipping ahead is a swipe away in the full player; what the bar cannot otherwise
            // reach is the queue, so that is the button worth the space.
            VayouIconButton(onClick = { isQueueOpen = true }) {
                Icon(
                    imageVector = VayouIcons.MusicPlaylist,
                    contentDescription = stringResource(R.string.queue),
                    tint = VayouTheme.colors.onSurface,
                )
            }
        },
    )

    // Outside the bar's own animation: the sheet belongs to the screen, not to the bar, and takes
    // the colours of whatever is behind it.
    if (isQueueOpen && controller != null) {
        QueueSheet(player = controller, onDismiss = { isQueueOpen = false })
    }
}

/** Playing, or about to be. Idle and ended are a session with nothing to say. */
private val ActiveStates = setOf(Player.STATE_READY, Player.STATE_BUFFERING)

private const val TickMs = 500L

/** A step below the list rows: the bar is a reminder of what is playing, not a row to scan. */
private val MiniArtworkSize = 40.dp
