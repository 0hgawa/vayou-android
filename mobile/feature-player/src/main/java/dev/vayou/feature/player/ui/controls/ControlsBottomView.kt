package dev.vayou.feature.player.ui.controls

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.VideoContentScale
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenuItem
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.ui.extensions.copy
import dev.vayou.core.player.extensions.formatted
import dev.vayou.feature.player.LocalControlsVisibilityState
import dev.vayou.feature.player.buttons.PlayerButton
import dev.vayou.feature.player.extensions.drawableRes
import dev.vayou.feature.player.extensions.nameRes
import dev.vayou.feature.player.extensions.noRippleClickable
import dev.vayou.feature.player.state.ABRepeatState
import dev.vayou.feature.player.state.MediaPresentationState
import dev.vayou.feature.player.state.durationFormatted
import dev.vayou.feature.player.state.pendingPositionFormatted
import dev.vayou.feature.player.state.positionFormatted
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    mediaPresentationState: MediaPresentationState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    isPipSupported: Boolean,
    abRepeatState: ABRepeatState,
    showAbRepeat: Boolean,
    onToggleAbRepeat: () -> Unit,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onRotateClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val controlsVisibilityState = LocalControlsVisibilityState.current
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    var overflowExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isPortrait) {
        if (!isPortrait) overflowExpanded = false
    }

    LaunchedEffect(overflowExpanded) {
        if (overflowExpanded) {
            controlsVisibilityState?.showControls(Duration.INFINITE)
        } else {
            controlsVisibilityState?.showControls()
        }
    }

    Column(
        modifier = modifier
            .padding(systemBarsPadding.copy(top = 0.dp))
            .padding(horizontal = 8.dp)
            .padding(top = 16.dp)
            .padding(bottom = 16.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (mediaPresentationState.isLive) {
            LiveIndicator(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var showPendingPosition by rememberSaveable { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.noRippleClickable {
                        showPendingPosition = !showPendingPosition
                    },
                ) {
                    Text(
                        text = when (showPendingPosition) {
                            true -> "-${mediaPresentationState.pendingPositionFormatted}"
                            false -> mediaPresentationState.positionFormatted
                        },
                        style = VayouTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Text(
                        text = " / ",
                        style = VayouTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Text(
                        text = mediaPresentationState.durationFormatted,
                        style = VayouTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                }

                AnimatedVisibility(
                    visible = showAbRepeat || abRepeatState.pointA != null,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ABRepeatButton(
                            label = abRepeatState.pointA?.let { "A ${it.milliseconds.formatted()}" } ?: "A",
                            isSet = abRepeatState.pointA != null,
                            onClick = abRepeatState::toggleA,
                        )
                        ABRepeatButton(
                            label = abRepeatState.pointB?.let { "B ${it.milliseconds.formatted()}" } ?: "B",
                            isSet = abRepeatState.pointB != null,
                            enabled = abRepeatState.pointA != null,
                            onClick = abRepeatState::toggleB,
                        )
                        if (abRepeatState.pointA != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { abRepeatState.reset() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = VayouIcons.Close,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }

            PlayerSeekbar(
                modifier = Modifier.padding(horizontal = 8.dp),
                position = mediaPresentationState.position.toFloat(),
                duration = mediaPresentationState.duration.toFloat(),
                abRepeatA = abRepeatState.pointA?.toFloat(),
                abRepeatB = abRepeatState.pointB?.toFloat(),
                onSeek = { onSeek(it.toLong()) },
                onSeekFinished = { onSeekEnd() },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = controlsAlignment),
            ) {
                PlayerButton(onClick = onSubtitleClick) {
                    Icon(
                        imageVector = VayouIcons.Caption,
                        contentDescription = null,
                    )
                }
                PlayerButton(onClick = onAudioClick) {
                    Icon(
                        imageVector = VayouIcons.Audio,
                        contentDescription = null,
                    )
                }
                if (!mediaPresentationState.isLive) {
                    PlayerButton(onClick = onPlaybackSpeedClick) {
                        val asInt = playbackSpeed.toInt()
                        val label = if (playbackSpeed == asInt.toFloat()) "${asInt}×"
                                    else "%.2f".format(playbackSpeed).trimEnd('0').trimEnd('.', ',') + "×"
                        Text(
                            text = label,
                            style = VayouTheme.typography.titleSmall,
                            color = Color.White,
                        )
                    }
                }
                if (isPortrait) {
                    Box {
                        PlayerButton(onClick = { overflowExpanded = !overflowExpanded }) {
                            Icon(
                                imageVector = VayouIcons.MoreHoriz,
                                contentDescription = null,
                                tint = if (overflowExpanded) VayouTheme.colors.accent else Color.White,
                            )
                        }
                        VayouDropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                        ) {
                            VayouDropdownMenuItem(
                                text = stringResource(R.string.controls_lock),
                                icon = VayouIcons.Lock,
                                onClick = {
                                    overflowExpanded = false
                                    onLockControlsClick()
                                },
                            )
                            VayouDropdownMenuItem(
                                text = stringResource(R.string.screen_rotation),
                                icon = VayouIcons.Rotation,
                                onClick = {
                                    overflowExpanded = false
                                    onRotateClick()
                                },
                            )
                            VayouDropdownMenuItem(
                                text = stringResource(videoContentScale.nameRes()),
                                icon = VayouIcons.Size,
                                onClick = {
                                    overflowExpanded = false
                                    onVideoContentScaleLongClick()
                                },
                            )
                            if (!mediaPresentationState.isLive) {
                                VayouDropdownMenuItem(
                                    text = stringResource(R.string.ab_repeat),
                                    icon = VayouIcons.Repeat,
                                    contentColor = if (showAbRepeat || abRepeatState.pointA != null) {
                                        VayouTheme.colors.accent
                                    } else {
                                        VayouTheme.colors.onSurface
                                    },
                                    onClick = { onToggleAbRepeat() },
                                )
                            }
                            if (isPipSupported) {
                                VayouDropdownMenuItem(
                                    text = stringResource(R.string.pip_settings),
                                    icon = VayouIcons.Pip,
                                    onClick = {
                                        overflowExpanded = false
                                        onPictureInPictureClick()
                                    },
                                )
                            }
                        }
                    }
                } else {
                    PlayerButton(onClick = onLockControlsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock_open),
                            contentDescription = null,
                        )
                    }
                    PlayerButton(onClick = onRotateClick) {
                        Icon(
                            imageVector = VayouIcons.Rotation,
                            contentDescription = null,
                        )
                    }
                    PlayerButton(
                        onClick = onVideoContentScaleClick,
                        onLongClick = onVideoContentScaleLongClick,
                    ) {
                        Icon(
                            painter = painterResource(videoContentScale.drawableRes()),
                            contentDescription = null,
                        )
                    }
                    if (!mediaPresentationState.isLive) {
                        PlayerButton(onClick = onToggleAbRepeat) {
                            Text(
                                text = "AB",
                                style = VayouTheme.typography.titleSmall,
                                color = if (showAbRepeat || abRepeatState.pointA != null) {
                                    VayouTheme.colors.accent
                                } else {
                                    Color.White
                                },
                            )
                        }
                    }
                    if (isPipSupported) {
                        PlayerButton(onClick = onPictureInPictureClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_pip),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            PlayerButton(onClick = onPlaylistClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_playlist),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun PlayerSeekbar(
    modifier: Modifier = Modifier,
    position: Float,
    duration: Float,
    abRepeatA: Float?,
    abRepeatB: Float?,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
) {
    val accentColor = VayouTheme.colors.accent
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(duration) {
                    coroutineScope {
                        launch {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek(fraction * duration)
                                onSeekFinished()
                            }
                        }
                        launch {
                            detectHorizontalDragGestures(
                                onDragEnd = { onSeekFinished() },
                            ) { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek(fraction * duration)
                            }
                        }
                    }
                },
        ) {
            val range = duration.takeIf { it > 0f } ?: 1f
            val fraction = (position / range).coerceIn(0f, 1f)
            val trackHeight = 4.dp.toPx()
            val centerY = size.height / 2f
            val trackY = centerY - trackHeight / 2f
            val trackRadius = CornerRadius(trackHeight / 2f)

            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackHeight),
                cornerRadius = trackRadius,
            )

            val activeWidth = size.width * fraction
            if (activeWidth > 0f) {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(0f, trackY),
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = trackRadius,
                )
            }

            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(size.width * fraction, centerY),
            )

            if (abRepeatA != null && abRepeatB != null) {
                val aX = size.width * (abRepeatA / range).coerceIn(0f, 1f)
                val bX = size.width * (abRepeatB / range).coerceIn(0f, 1f)
                drawRect(
                    color = Color.White.copy(alpha = 0.3f),
                    topLeft = Offset(aX, trackY),
                    size = Size(bX - aX, trackHeight),
                )
                val tickWidth = 2.dp.toPx()
                drawRect(color = Color.White.copy(alpha = 0.9f), topLeft = Offset(aX, trackY), size = Size(tickWidth, trackHeight))
                drawRect(color = Color.White.copy(alpha = 0.9f), topLeft = Offset(bX - tickWidth, trackY), size = Size(tickWidth, trackHeight))
            } else if (abRepeatA != null) {
                val aX = size.width * (abRepeatA / range).coerceIn(0f, 1f)
                drawRect(color = Color.White.copy(alpha = 0.9f), topLeft = Offset(aX, trackY), size = Size(2.dp.toPx(), trackHeight))
            }
        }
    }
}

@Composable
private fun LiveIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(VayouTheme.colors.accent, CircleShape),
        )
        Text(
            text = stringResource(R.string.player_live),
            style = VayouTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun ABRepeatButton(
    label: String,
    isSet: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val accentColor = VayouTheme.colors.accent
    val alpha = if (enabled) 1f else 0.35f
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSet) accentColor.copy(alpha = alpha) else Color.White.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = if (isSet) accentColor.copy(alpha = alpha) else Color.White.copy(alpha = 0.4f * alpha),
                shape = RoundedCornerShape(6.dp),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = VayouTheme.typography.labelMedium,
            color = Color.White.copy(alpha = alpha),
        )
    }
}
