package dev.vayou.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The only control left while the screen is locked.
 *
 * No scrim and nothing beside it, because the point of locking is to watch without the film being
 * covered. It answers the same tap the controls answer when unlocked, so the way to find it is the
 * way the viewer already knows.
 */
@Composable
fun UnlockButton(visible: Boolean, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        IconButton(
            onClick = onUnlock,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(Inset)
                .size(ButtonSize)
                .background(VayouTheme.colors.videoPlate, CircleShape),
        ) {
            Icon(
                imageVector = VayouIcons.Lock,
                contentDescription = stringResource(R.string.unlock),
                tint = VayouTheme.colors.onVideo,
            )
        }
    }
}

/** Dark rather than light, so it reads against a bright film as well as a dark one. */
private val ButtonSize = 48.dp

private val Inset = 20.dp
