package dev.vayou.core.ui.designsystem.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons

/**
 * The arrow that leaves a screen.
 *
 * Every app bar opens with one, and spelling out the button, the glyph and the description at each
 * of them is as many chances for one to lose its label.
 */
@Composable
fun VayouBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** White where the bar is drawn straight onto artwork and the palette cannot be trusted. */
    contentColor: Color = LocalContentColor.current,
    contentDescription: String = stringResource(R.string.navigate_up),
) {
    VayouIconButton(onClick = onClick, modifier = modifier, contentColor = contentColor) {
        Icon(imageVector = VayouIcons.ArrowBack, contentDescription = contentDescription)
    }
}
