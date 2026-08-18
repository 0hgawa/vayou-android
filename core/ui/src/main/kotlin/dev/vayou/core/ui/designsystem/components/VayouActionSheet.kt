package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import dev.vayou.core.ui.theme.VayouTheme

/**
 * What an overflow button opens: the actions for one item, as a sheet rather than a dropdown.
 *
 * A dropdown anchors to the row that was tapped, so on a tall screen its items land wherever that
 * row happened to be -- often out of a thumb's reach. This always opens at the bottom. It also names
 * the item it acts on, which a dropdown cannot: tapping "Remove" in a menu that never said what it
 * belongs to is a bet on having hit the right row.
 */
@Composable
fun VayouActionSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VayouSheetDefaults.HorizontalPadding)
                .padding(
                    top = VayouSheetDefaults.TitleTopPadding,
                    bottom = VayouSheetDefaults.TitleBottomPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.md),
        ) {
            leading?.invoke()
            Column {
                Text(
                    text = title,
                    // The weight every other sheet heads itself with. This one carried a leading
                    // thumbnail and a subtitle, so it was built by hand and then set a size smaller
                    // than the plain ones -- two sheets a tap apart, titled differently.
                    style = VayouSheetDefaults.TitleStyle,
                    color = VayouTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = VayouTheme.typography.bodyMedium,
                        color = VayouTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        content()
    }
}

/**
 * One action. Full width, so the whole row is the target rather than just the label.
 *
 * Twelve of vertical padding and not sixteen: a 24dp line inside 16dp makes a 56dp row, and eight of
 * those is a sheet that scrolls for no reason. At 48 the list still clears the touch-target floor
 * with nothing to spare, which is the smallest a row goes without costing reach.
 */
@Composable
fun VayouActionSheetItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    VayouSheetRow(text = text, onClick = onClick, leading = { VayouSheetRowIcon(icon) })
}
