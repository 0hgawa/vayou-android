package dev.vayou.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberNextButtonState
import dev.vayou.core.ui.R as coreUiR
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.feature.player.LocalControlsVisibilityState

@OptIn(UnstableApi::class)
@Composable
internal fun NextButton(player: Player, modifier: Modifier = Modifier) {
    val state = rememberNextButtonState(player)
    val controlsVisibilityState = LocalControlsVisibilityState.current

    PlayerButton(
        modifier = modifier.size(PlayerButtonSize.Standard),
        isEnabled = state.isEnabled,
        onClick = {
            state.onClick()
            controlsVisibilityState?.showControls()
        },
    ) {
        Icon(
            painter = painterResource(coreUiR.drawable.ic_skip_next),
            contentDescription = stringResource(coreUiR.string.player_controls_next),
            modifier = Modifier.size(VayouTheme.iconSize.lg),
        )
    }
}
