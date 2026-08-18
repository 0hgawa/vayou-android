package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The app's filled button.
 *
 * Not Material's: two implementations meant two heights and two paddings, so a dialog's confirm
 * stood taller than the dismiss beside it — which is the pair that has to read as one row more
 * than any other in the app.
 */
@Composable
fun VayouButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = VayouTheme.colors.accent,
    contentColor: Color = VayouTheme.colors.onAccent,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        // A faint wash of the same colour rather than the palette's grey: disabled should read as
        // the button waiting, not as a different, heavier component.
        LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else DisabledContentAlpha),
        LocalTextStyle provides VayouTheme.typography.labelLarge,
    ) {
        Box(
            modifier = modifier
                .defaultMinSize(minHeight = MinHeight)
                .clip(VayouTheme.shapes.full)
                .background(containerColor.copy(alpha = if (enabled) 1f else DisabledContainerAlpha))
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = FilledHorizontal, vertical = Vertical),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * The quiet half of a pair — cancel, close, dismiss.
 *
 * [contentColor] defaults to the muted role rather than the accent: the action you are being
 * steered away from should not be the most coloured thing in the dialog.
 */
@Composable
fun VayouTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = VayouTheme.colors.onSurfaceVariant,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else DisabledContentAlpha),
        LocalTextStyle provides VayouTheme.typography.labelLarge,
    ) {
        Box(
            modifier = modifier
                // The same floor the filled button takes, so a pair of them is one row and not two
                // heights. Its horizontal padding is smaller: a text button has no shape to fill.
                .defaultMinSize(minHeight = MinHeight)
                .clip(VayouTheme.shapes.small)
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = TextHorizontal, vertical = Vertical),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** What both buttons stand on, so a dialog's pair reads as one row of actions. */
private val MinHeight = 40.dp

private val FilledHorizontal = 24.dp

private val TextHorizontal = 12.dp

private val Vertical = 10.dp

private const val DisabledContentAlpha = 0.35f

private const val DisabledContainerAlpha = 0.08f
