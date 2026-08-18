package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons

/**
 * A glyph you can press.
 *
 * A box rather than Material's own icon button, which wraps a Surface and a minimum-size modifier
 * around the same thing and gives no way to set a container colour without a whole colours object.
 *
 * [modifier] may set its own size; the 48dp here is the floor a bare glyph gets, not a cap. A size
 * from the caller constrains this one, which is how the player asks for its larger transport keys.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VayouIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /**
     * A second, slower answer from the same glyph -- the player's framing button cycles on a tap
     * and offers the whole list on a hold, rather than making the reader cycle past what they want.
     */
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalContentColor.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else DisabledAlpha)) {
        Box(
            modifier = modifier
                .size(VayouIconButtonSize)
                .clip(CircleShape)
                .background(containerColor)
                .combinedClickable(
                    // Left for the ripple to make on first touch. A row of these is eight buttons,
                    // and most of them are never pressed.
                    interactionSource = null,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onLongClick = onLongClick,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** The floor a bare glyph gets, and the width of every icon button in the app. */
val VayouIconButtonSize = 48.dp

private const val DisabledAlpha = 0.38f

/**
 * The three dots on a **list row**. Narrower than a plain [VayouIconButton], so the ripple reads as
 * a slim pill instead of a wide circle and the row's text is not held further from its trailing
 * edge than it needs to be.
 *
 * Not for a bar, where the button is the last thing before the edge of the screen.
 */
@Composable
fun VayouOverflowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Named by default: a reader that reaches three dots has nothing else to go on. */
    contentDescription: String? = stringResource(R.string.more_options),
    tint: Color? = null,
) {
    VayouIconButton(onClick = onClick, modifier = modifier.width(OverflowButtonWidth)) {
        Icon(
            imageVector = VayouIcons.MoreVert,
            contentDescription = contentDescription,
            tint = tint ?: LocalContentColor.current,
        )
    }
}

private val OverflowButtonWidth = 36.dp
