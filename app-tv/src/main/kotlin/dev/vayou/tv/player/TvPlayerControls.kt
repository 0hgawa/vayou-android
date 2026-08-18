package dev.vayou.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.player.ui.asSpeedLabel
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.tv.R
import dev.vayou.tv.TvControlButton
import dev.vayou.tv.TvControlCapsule
import dev.vayou.tv.TvSeekBar
import dev.vayou.tv.tvClock

/**
 * The controls over a film on a television.
 *
 * Buttons, and they are reached with the D-pad rather than pressed where they sit. Play and pause
 * takes the focus the moment the panel appears, because it is the one a viewer came for; from
 * there left and right walk the row, and the seek bar is a step above it.
 *
 * Darkened at both ends rather than across the middle: the panel is at the foot and the name is at
 * the head, and what is between them is the film.
 */
@Composable
internal fun TvPlayerControls(
    isPlaying: Boolean,
    isLive: Boolean,
    speed: Float,
    positionMs: Long,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpen: (TvSelector) -> Unit,
) {
    val playPauseFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playPauseFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to ScrimHead,
                    HeadEnd to Color.Transparent,
                    FootStart to Color.Transparent,
                    1f to ScrimFoot,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = SideInset, vertical = FootInset),
            verticalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            if (isLive) {
                LiveBadge()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RowGap),
                ) {
                    Text(
                        text = tvClock(positionMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                    TvSeekBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = tvClock(durationMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = SpentAlpha),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RowGap),
            ) {
                TvControlButton(
                    icon = if (isPlaying) VayouIcons.PauseFilled else VayouIcons.Play,
                    label = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    onClick = onPlayPause,
                    modifier = Modifier.focusRequester(playPauseFocus),
                )
                // The two of them in one capsule, the way a television's own player draws them: they
                // are two halves of one question -- which item -- and a bubble each would read as
                // two more buttons in a row that already has six. Focus fills the half it is on.
                //
                // Left out rather than dimmed at the ends of a queue: a D-pad walks past a disabled
                // button as if it were not there, so a dim one is a gap with a picture in it.
                if (hasPrevious || hasNext) {
                    TvControlCapsule {
                        if (hasPrevious) {
                            TvControlButton(
                                icon = VayouIcons.SkipPreviousFilled,
                                label = stringResource(R.string.previous),
                                onClick = onPrevious,
                                isGrouped = true,
                            )
                        }
                        if (hasNext) {
                            TvControlButton(
                                icon = VayouIcons.SkipNextFilled,
                                label = stringResource(R.string.next),
                                onClick = onNext,
                                isGrouped = true,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // One capsule for everything that changes how this film plays -- its sound, its
                // words, its speed, and the drawer holding the rest. They are one idea, and four
                // bubbles for it read as four unrelated buttons.
                TvControlCapsule {
                    // Subtitles first, then sound, then speed -- the order the phone's bar has had
                    // all along. Which words appear is the thing most often reached for, and a
                    // control in a different place on each device is a control found twice.
                    TvControlButton(
                        icon = VayouIcons.Caption,
                        label = stringResource(R.string.subtitles),
                        onClick = { onOpen(TvSelector.Subtitle) },
                        isGrouped = true,
                    )
                    TvControlButton(
                        icon = VayouIcons.Audio,
                        label = stringResource(R.string.audio_track),
                        onClick = { onOpen(TvSelector.Audio) },
                        isGrouped = true,
                    )
                    // Speed means nothing on a channel: it arrives as fast as it arrives.
                    if (!isLive) {
                        // The number rather than a dial, as the phone's button has always had it:
                        // "1.5x" is shorter than any picture of it and says more.
                        TvControlButton(
                            text = speed.asSpeedLabel(),
                            label = stringResource(R.string.speed),
                            onClick = { onOpen(TvSelector.Speed) },
                            isGrouped = true,
                        )
                    }
                    TvControlButton(
                        icon = VayouIcons.MoreHoriz,
                        label = stringResource(R.string.more),
                        onClick = { onOpen(TvSelector.More) },
                        isGrouped = true,
                    )
                }
                // On its own at the end of the row, and outside that capsule: what is playing next
                // is the one thing here that is about the evening rather than about this film. A
                // channel has no running order to show.
                if (!isLive) {
                    TvControlButton(
                        icon = VayouIcons.Queue,
                        label = stringResource(R.string.playlist),
                        onClick = { onOpen(TvSelector.Playlist) },
                    )
                }
            }
        }
    }
}

/** A channel has no length and nothing to scrub, so it says what it is instead. */
@Composable
private fun LiveBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BadgeGap),
    ) {
        Box(
            modifier = Modifier
                .size(BadgeDot)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(
            text = stringResource(R.string.live),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

private val ScrimHead = Color.Black.copy(alpha = 0.55f)

private val ScrimFoot = Color.Black.copy(alpha = 0.75f)

private const val HeadEnd = 0.3f

private const val FootStart = 0.7f

private const val SpentAlpha = 0.7f

private val SideInset = 48.dp

private val FootInset = 32.dp

private val RowGap = 12.dp

private val BadgeGap = 8.dp

private val BadgeDot = 8.dp
