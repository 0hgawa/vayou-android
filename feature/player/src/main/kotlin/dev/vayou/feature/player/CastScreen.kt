package dev.vayou.feature.player

import android.content.ComponentName
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import com.google.common.util.concurrent.MoreExecutors
import dev.vayou.core.player.PlaybackService
import dev.vayou.core.player.stepToNext
import dev.vayou.core.player.stepToPrevious
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.AlongTheTimeline
import dev.vayou.core.ui.designsystem.components.VayouBackButton
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.theme.VayouTheme

/**
 * What the phone shows while the film is on a television.
 *
 * The whole screen and not a badge over the player, which is what the old app does and what the
 * situation asks for: there is no picture here to frame. The surface would be a black rectangle
 * receiving nothing, the framing and zoom controls would act on nothing, and a brightness gesture
 * would dim a screen nobody is watching.
 *
 * The same controller drives it. Casting swaps the player inside the service, not the session, so
 * everything here is the transport that was already there -- pointed at a room instead of a pane.
 */
@Composable
internal fun CastScreen(player: Player, deviceName: String?, onBack: () -> Unit) {
    // The bars come back. The player hides them because it is showing a picture edge to edge; this
    // is an ordinary screen with a back button on it, and taking the navigation away from it leaves
    // a gesture as the only way out.
    val activity = LocalActivity.current as? PlayerActivity
    LaunchedEffect(activity) { activity?.showSystemBars(true) }

    val playPause = rememberPlayPauseButtonState(player)
    val previous = rememberPreviousButtonState(player)
    val next = rememberNextButtonState(player)
    // Enabled at the ends too, because there the button turns the queue over rather than doing
    // nothing -- see [stepToNext].
    val isQueued = player.mediaItemCount > 1
    val progress = rememberProgressStateWithTickInterval(player = player, tickIntervalMs = ProgressTickMs)
    val title = rememberTitleState(player).title.orEmpty()
    // Whether the television has something to show yet, not whether the transport says play. It
    // buffers mid-film too, and the spinner is the only thing on this screen that reports it.
    val isWaiting = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VayouTheme.colors.videoBackdrop)
            .safeDrawingPadding()
            .padding(horizontal = SideInset),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            VayouBackButton(onClick = onBack, contentColor = VayouTheme.colors.onVideo)
            Text(
                text = title,
                style = VayouTheme.typography.titleMedium,
                color = VayouTheme.colors.onVideo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TitleGap),
            )
            CastButton(onVideo = true)
        }

        // The room, named, where the picture would be. Nothing else belongs in the middle: the one
        // thing worth saying is which television has it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isWaiting) {
                VayouCircularProgress(size = MarkSize, color = VayouTheme.colors.onVideoMuted)
            } else {
                Icon(
                    imageVector = VayouIcons.Cast,
                    contentDescription = null,
                    tint = VayouTheme.colors.onVideoMuted,
                    modifier = Modifier.size(MarkSize),
                )
            }
            Spacer(modifier = Modifier.size(MarkGap))
            Text(
                text = deviceName ?: stringResource(R.string.cast),
                style = VayouTheme.typography.bodyLarge,
                color = VayouTheme.colors.onVideoMuted,
                textAlign = TextAlign.Center,
            )
        }

        PlayerSeekBar(
            positionMs = progress.currentPositionMs,
            durationMs = progress.durationMs,
            onSeek = player::seekTo,
        )

        AlongTheTimeline {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = BottomInset),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CastTransportButton(
                    glyph = VayouIcons.SkipPreviousFilled,
                    label = R.string.previous_file,
                    enabled = previous.isEnabled || isQueued,
                ) {
                    player.stepToPrevious()
                }
                CastTransportButton(VayouIcons.Replay, R.string.cast_skip_back) {
                    player.seekTo((player.currentPosition - SkipMs).coerceAtLeast(0L))
                }
                PlayerButton(onClick = playPause::onClick, size = PlayerButtonSize.Primary) {
                    Icon(
                        imageVector = if (playPause.showPlay) VayouIcons.Play else VayouIcons.PauseFilled,
                        contentDescription = stringResource(if (playPause.showPlay) R.string.play else R.string.pause),
                        modifier = Modifier.size(PlayerButtonSize.PrimaryGlyph),
                    )
                }
                CastTransportButton(VayouIcons.FastForward, R.string.cast_skip_forward) {
                    player.seekTo((player.currentPosition + SkipMs).coerceAtMost(player.duration))
                }
                CastTransportButton(
                    glyph = VayouIcons.SkipNextFilled,
                    label = R.string.next_file,
                    enabled = next.isEnabled || isQueued,
                ) {
                    player.stepToNext()
                }
            }
        }
    }
}

@Composable
private fun CastTransportButton(
    glyph: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PlayerButton(onClick = onClick, size = PlayerButtonSize.Secondary, enabled = enabled) {
        Icon(
            imageVector = glyph,
            contentDescription = stringResource(label),
            modifier = Modifier.size(PlayerButtonSize.StandardGlyph),
        )
    }
}

/**
 * A controller on the playback session, held only while this screen is started.
 *
 * Its own rather than the player screen's: this bar outlives that screen, which is the whole point
 * of it.
 *
 * [isWanted] is false for a caller with nothing to show yet. Connecting is neither free nor passive:
 * binding to the session *starts* the service, and the service builds the player, the extra decoders
 * and, on a phone that can cast, the whole cast framework. A bar that appears only while a
 * television is selected should not be paying for that on a phone that never casts.
 */
@Composable
internal fun rememberPlaybackController(isWanted: Boolean = true): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    LifecycleStartEffect(isWanted) {
        if (!isWanted) return@LifecycleStartEffect onStopOrDispose { controller = null }
        val token = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(context.applicationContext, token).buildAsync()
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            MoreExecutors.directExecutor(),
        )
        onStopOrDispose {
            controller = null
            MediaController.releaseFuture(future)
        }
    }
    return controller
}

/**
 * The transport alone, for a player reopened from the cast bar.
 *
 * That bar brings the activity forward with no file named, because the session already holds the
 * film -- and asking for it again would set the queue a second time and start it over. With no
 * television selected there is nothing here to show, so it closes.
 */
@Composable
internal fun CastOnlyScreen(onBack: () -> Unit) {
    val route = rememberSelectedRoute()
    val controller = rememberPlaybackController()

    LaunchedEffect(route) { if (route == null) onBack() }
    if (route == null || controller == null) return

    CastScreen(player = controller, deviceName = route.name, onBack = onBack)
}

/** The same fifteen the old player jumped by from this screen. */
private const val SkipMs = 15_000L

/** A second: this drives a clock and a bar for a picture in another room, and nothing here moves
 *  fast enough to want more. */
private const val ProgressTickMs = 1_000L

private val SideInset = 24.dp

private val BottomInset = 24.dp

private val TitleGap = 8.dp

private val MarkSize = 48.dp

private val MarkGap = 8.dp
