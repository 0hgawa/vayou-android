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
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import dev.vayou.core.ui.R as coreUiR

@OptIn(UnstableApi::class)
@Composable
fun PlayPauseButton(player: Player, modifier: Modifier = Modifier) {
    val state = rememberPlayPauseButtonState(player)
    val icon = when (state.showPlay) {
        true -> painterResource(coreUiR.drawable.ic_play)
        false -> painterResource(coreUiR.drawable.ic_pause)
    }
    val contentDescription = stringResource(coreUiR.string.play_pause)

    PlayerButton(
        modifier = modifier.size(PlayerButtonSize.Primary),
        isEnabled = state.isEnabled,
        onClick = state::onClick,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(PlayerButtonSize.PrimaryIcon),
        )
    }
}
