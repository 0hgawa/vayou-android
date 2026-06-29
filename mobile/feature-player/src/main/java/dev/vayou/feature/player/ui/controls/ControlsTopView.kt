package dev.vayou.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberRepeatButtonState
import androidx.media3.ui.compose.state.rememberShuffleButtonState
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenuItem
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.ui.extensions.copy
import dev.vayou.feature.player.LocalControlsVisibilityState
import dev.vayou.feature.player.buttons.PlayerButton
import dev.vayou.feature.player.cast.CastButton
import kotlin.time.Duration

@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    player: Player,
    title: String,
    showCastButton: Boolean = false,
    isNightModeEnabled: Boolean = false,
    onNightModeToggle: () -> Unit = {},
    sleepTimerFormattedRemaining: String? = null,
    onSleepTimerClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onPlayInBackgroundClick: () -> Unit = {},
    onBackClick: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val controlsVisibilityState = LocalControlsVisibilityState.current
    val repeatButtonState = rememberRepeatButtonState(player)
    val shuffleButtonState = rememberShuffleButtonState(player)
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(menuExpanded) {
        if (menuExpanded) {
            controlsVisibilityState?.showControls(Duration.INFINITE)
        } else {
            controlsVisibilityState?.showControls()
        }
    }

    Row(
        modifier = modifier
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 8.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlayerButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
            )
        }
        Text(
            text = title,
            style = VayouTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showCastButton) {
            CastButton(inPlayerOverlay = true)
        }
        Box {
            PlayerButton(onClick = { menuExpanded = !menuExpanded }) {
                Icon(
                    imageVector = VayouIcons.MoreVert,
                    contentDescription = null,
                )
            }
            VayouDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                val repeatText = when (repeatButtonState.repeatModeState) {
                    Player.REPEAT_MODE_OFF -> stringResource(R.string.loop_mode_off)
                    Player.REPEAT_MODE_ONE -> stringResource(R.string.loop_mode_one)
                    else -> stringResource(R.string.loop_mode_all)
                }
                VayouDropdownMenuItem(
                    text = repeatText,
                    icon = VayouIcons.Repeat,
                    contentColor = if (repeatButtonState.repeatModeState != Player.REPEAT_MODE_OFF) {
                        VayouTheme.colors.accent
                    } else {
                        VayouTheme.colors.onSurface
                    },
                    onClick = { repeatButtonState.onClick() },
                )
                VayouDropdownMenuItem(
                    text = stringResource(
                        if (shuffleButtonState.shuffleOn) R.string.shuffle_on else R.string.shuffle_off,
                    ),
                    icon = VayouIcons.Shuffle,
                    contentColor = if (shuffleButtonState.shuffleOn) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                    onClick = { shuffleButtonState.onClick() },
                )
                VayouDropdownMenuItem(
                    text = sleepTimerFormattedRemaining ?: stringResource(R.string.sleep_timer),
                    icon = VayouIcons.Timer,
                    contentColor = if (sleepTimerFormattedRemaining != null) {
                        VayouTheme.colors.accent
                    } else {
                        VayouTheme.colors.onSurface
                    },
                    onClick = {
                        menuExpanded = false
                        onSleepTimerClick()
                    },
                )
                VayouDropdownMenuItem(
                    text = stringResource(R.string.background_play),
                    icon = VayouIcons.Background,
                    onClick = {
                        menuExpanded = false
                        onPlayInBackgroundClick()
                    },
                )
                VayouDropdownMenuItem(
                    text = stringResource(
                        if (isNightModeEnabled) R.string.night_mode_on else R.string.night_mode,
                    ),
                    icon = VayouIcons.DarkMode,
                    contentColor = if (isNightModeEnabled) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                    onClick = { onNightModeToggle() },
                )
                VayouDropdownMenuItem(
                    text = stringResource(R.string.equalizer),
                    icon = VayouIcons.Equalizer,
                    onClick = {
                        menuExpanded = false
                        onEqualizerClick()
                    },
                )
            }
        }
    }
}
