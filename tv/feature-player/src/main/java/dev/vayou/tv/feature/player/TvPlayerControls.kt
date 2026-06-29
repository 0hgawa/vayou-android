package dev.vayou.tv.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.ui.res.painterResource
import dev.vayou.core.ui.R as coreUiR
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
fun TvPlayerControls(
    visible: Boolean,
    isPlaying: Boolean,
    isLive: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onAudioTracks: () -> Unit,
    onSubtitleTracks: () -> Unit,
    onSpeed: () -> Unit,
    onPlaylist: () -> Unit,
    onMore: () -> Unit,
    playbackSpeed: Float,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.3f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f),
                    ),
                ),
        ) {
            BottomControlsPanel(
                isPlaying = isPlaying,
                isLive = isLive,
                positionMs = positionMs,
                durationMs = durationMs,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
                onAudioTracks = onAudioTracks,
                onSubtitleTracks = onSubtitleTracks,
                onSpeed = onSpeed,
                onPlaylist = onPlaylist,
                onMore = onMore,
                playbackSpeed = playbackSpeed,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

@Composable
private fun BottomControlsPanel(
    isPlaying: Boolean,
    isLive: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onAudioTracks: () -> Unit,
    onSubtitleTracks: () -> Unit,
    onSpeed: () -> Unit,
    onPlaylist: () -> Unit,
    onMore: () -> Unit,
    playbackSpeed: Float,
    modifier: Modifier = Modifier,
) {
    val playPauseFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playPauseFocus.requestFocus() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isLive) {
            LiveBadge()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = formatTime(positionMs),
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
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvControlButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                onClick = onPlayPause,
                big = true,
                modifier = Modifier.focusRequester(playPauseFocus),
            )
            if (hasPrevious) {
                TvControlButton(
                    icon = Icons.Filled.SkipPrevious,
                    contentDescription = "Anterior",
                    onClick = onPrevious,
                )
            }
            if (hasNext) {
                TvControlButton(
                    icon = Icons.Filled.SkipNext,
                    contentDescription = "Próximo",
                    onClick = onNext,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TvPillGroup {
                SeekGroupButton(
                    icon = VayouIcons.Audio,
                    contentDescription = "Áudio",
                    onClick = onAudioTracks,
                )
                SeekGroupButton(
                    icon = VayouIcons.Caption,
                    contentDescription = "Legendas",
                    onClick = onSubtitleTracks,
                )
                if (!isLive) {
                    SeekGroupSlot(
                        contentDescription = "Velocidade",
                        onClick = onSpeed,
                    ) {
                        Text(
                            text = formatSpeed(playbackSpeed),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                SeekGroupButton(
                    icon = Icons.Outlined.MoreHoriz,
                    contentDescription = "Mais",
                    onClick = onMore,
                )
            }
            if (!isLive) {
                Spacer(modifier = Modifier.width(12.dp))
                TvPillGroup {
                    SeekGroupSlot(
                        contentDescription = "Lista",
                        onClick = onPlaylist,
                    ) {
                        Icon(
                            painter = painterResource(coreUiR.drawable.ic_playlist),
                            contentDescription = null,
                            modifier = Modifier.size(VayouTheme.iconSize.md),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(
            text = "AO VIVO",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun TvSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    var previewMs by remember { mutableLongStateOf(positionMs) }
    LaunchedEffect(positionMs, isFocused) {
        if (!isFocused) previewMs = positionMs
    }

    val displayMs = if (isFocused) previewMs else positionMs
    val fraction = if (durationMs > 0L) (displayMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val trackHeight by animateDpAsState(
        targetValue = if (isFocused) SeekBarFocusedTrack else SeekBarRestingTrack,
        animationSpec = tween(durationMillis = SeekBarTrackAnimMs),
        label = "seekbar-track-height",
    )

    val cs = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SeekBarHitHeight)
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (durationMs <= 0L) return@onPreviewKeyEvent false
                val isArrow = event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                if (!isArrow) return@onPreviewKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        val direction = if (event.key == Key.DirectionLeft) -1 else 1
                        val step = stepForRepeat(event.nativeKeyEvent.repeatCount)
                        previewMs = (previewMs + direction * step).coerceIn(0L, durationMs)
                        true
                    }
                    KeyEventType.KeyUp -> {
                        onSeek(previewMs)
                        true
                    }
                    else -> false
                }
            },
    ) {
        val totalWidth = maxWidth
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
        )
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(cs.primary),
            )
        }
        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = totalWidth * fraction - SeekBarThumb / 2)
                    .size(SeekBarThumb)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

private fun stepForRepeat(repeat: Int): Long = when {
    repeat < 2 -> 5_000L
    repeat < 6 -> 15_000L
    repeat < 12 -> 30_000L
    else -> 60_000L
}

private val SeekBarHitHeight = 20.dp
private val SeekBarRestingTrack = 4.dp
private val SeekBarFocusedTrack = 6.dp
private val SeekBarThumb = 16.dp
private const val SeekBarTrackAnimMs = 150

@Composable
private fun TvPillGroup(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun SeekGroupButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    SeekGroupSlot(contentDescription = contentDescription, onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(VayouTheme.iconSize.md),
        )
    }
}

@Composable
private fun SeekGroupSlot(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

private fun formatSpeed(speed: Float): String {
    val asInt = speed.toInt()
    if (speed == asInt.toFloat()) return "${asInt}×"
    return "%.2f".format(speed).trimEnd('0').trimEnd('.', ',') + "×"
}

@Composable
fun TvControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    big: Boolean = false,
) {
    val size = if (big) 56.dp else 48.dp
    val iconSize = if (big) VayouTheme.iconSize.lg else VayouTheme.iconSize.md
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
