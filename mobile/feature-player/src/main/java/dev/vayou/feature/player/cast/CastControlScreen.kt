package dev.vayou.feature.player.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.common.Utils
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouSlider
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.feature.player.buttons.PlayerButtonSize
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun CastControlScreen(
    onBackClick: () -> Unit,
) {
    val player = CastSessionManager.castPlayer
    val deviceName = CastSessionManager.deviceName

    if (player == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.no_active_cast_session),
                color = Color.White,
            )
        }
        return
    }

    val title = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: ""

    var isPlaying by remember { mutableStateOf(player.playWhenReady) }
    var isBuffering by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isSeeking) {
                position = player.currentPosition
                duration = player.duration.coerceAtLeast(0)
                sliderPosition = if (duration > 0) position.toFloat() / duration else 0f
            }
            isPlaying = player.playWhenReady
            isBuffering = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VayouIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = VayouIcons.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                style = VayouTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            CastButton(inPlayerOverlay = true)
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isBuffering) {
                    VayouCircularProgress(
                        size = 48.dp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                } else {
                    Icon(
                        imageVector = VayouIcons.Cast,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.5f),
                    )
                }
                Text(
                    text = deviceName ?: stringResource(R.string.cast),
                    style = VayouTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VayouIconButton(onClick = { player.seekToPrevious() }) {
                Icon(
                    imageVector = VayouIcons.SkipPrevious,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
            }
            VayouIconButton(
                onClick = {
                    val newPos = (player.currentPosition - 15000).coerceAtLeast(0)
                    player.seekTo(newPos)
                },
            ) {
                Icon(
                    imageVector = VayouIcons.Replay,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
            }
            Box(
                modifier = Modifier
                    .size(PlayerButtonSize.PrimaryHero)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                VayouIconButton(
                    contentColor = Color.Black,
                    onClick = {
                        isPlaying = !isPlaying
                        if (player.playWhenReady) player.pause() else player.play()
                    },
                ) {
                    Icon(
                        imageVector = if (isPlaying) VayouIcons.Pause else VayouIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            VayouIconButton(
                onClick = {
                    val newPos = (player.currentPosition + 15000).coerceAtMost(player.duration)
                    player.seekTo(newPos)
                },
            ) {
                Icon(
                    imageVector = VayouIcons.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
            }
            VayouIconButton(onClick = { player.seekToNext() }) {
                Icon(
                    imageVector = VayouIcons.SkipNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        VayouSlider(
            value = sliderPosition,
            onValueChange = {
                isSeeking = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                player.seekTo((sliderPosition * duration).toLong())
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = Utils.formatDurationMillis(position),
                style = VayouTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = Utils.formatDurationMillis(duration),
                style = VayouTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
