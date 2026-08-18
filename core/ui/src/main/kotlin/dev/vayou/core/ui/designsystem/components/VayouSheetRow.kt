package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A line in a bottom sheet: something to pick, or somewhere to go.
 *
 * One row for every sheet in the app. There were three of these -- the player's, the action sheet's
 * and the sort sheet's -- and they had drifted to two label sizes and two gaps, so the same list of
 * options was 14sp beside a film and 16sp beside a folder. Nobody sees them side by side; everybody
 * feels that the app is two apps.
 *
 * [selected] set makes the row report itself as one of a set rather than as a button, which is what
 * a screen reader announces and what the leading mark is drawn for.
 */
@Composable
fun VayouSheetRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    /** Drawn before the label, on the column every other row's label starts after. */
    leading: (@Composable () -> Unit)? = null,
    /** Drawn after it -- a chevron, a remaining time, which way a sort runs. */
    trailing: (@Composable () -> Unit)? = null,
    maxLines: Int = 1,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (selected == null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier.selectable(selected = selected, onClick = onClick)
                },
            )
            // Twelve of vertical padding and not sixteen: a 24dp line inside 16dp makes a 56dp row,
            // and eight of those is a sheet that scrolls for no reason. The floor is held by the
            // minimum height rather than by the padding, so the padding can stay on the grid.
            .heightIn(min = VayouSheetDefaults.RowMinHeight)
            .padding(horizontal = VayouSheetDefaults.HorizontalPadding, vertical = VayouTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = VayouTheme.typography.bodyLarge,
            color = VayouTheme.colors.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * A glyph at either end of a [VayouSheetRow].
 *
 * Here rather than at each call site so that the size and the tint cannot drift row by row -- which
 * is how one sheet ended up with 24dp icons and the next with 20dp ones.
 */
@Composable
fun VayouSheetRowIcon(icon: ImageVector, tint: Color = VayouTheme.colors.onSurface, isSmall: Boolean = false) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(if (isSmall) VayouTheme.iconSize.sm else VayouTheme.iconSize.md),
    )
}
