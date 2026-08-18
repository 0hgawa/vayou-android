package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The line above a list that says what it is -- the name of a section, or the order the list is in
 * when that order can be changed.
 *
 * One component for both because they are one thing on screen: same weight, same colour, same
 * inset, same optional action on the right. Kept apart they drift.
 *
 * Weighted like a label and not like content: it introduces what follows, and at title strength the
 * list would look like it starts here.
 */
@Composable
fun VayouListHeader(
    label: String,
    modifier: Modifier = Modifier,
    /** Non-null makes the label the current order, and tapping it opens the sheet that changes it. */
    isAscending: Boolean? = null,
    onClick: (() -> Unit)? = null,
    /** The list's own margin. Zero where the host already insets the slot this lands in. */
    outerInset: Dp = VayouTheme.spacing.sm,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = outerInset, vertical = OuterVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .clip(VayouTheme.shapes.full)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                // Completes the caller's margin: the arrow -- or the first letter of a plain label
                // -- lands on the left edge of the artwork in the rows below.
                .padding(horizontal = VayouTheme.spacing.sm, vertical = InnerVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
        ) {
            if (isAscending != null) {
                Icon(
                    imageVector = if (isAscending) VayouIcons.ArrowUpward else VayouIcons.ArrowDownward,
                    contentDescription = null,
                    tint = VayouTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(ArrowSize),
                )
            }
            Text(
                text = label,
                // labelLarge and not labelMedium: this line is a control, not a caption. What makes
                // it quiet is the colour, not the size.
                style = VayouTheme.typography.labelLarge,
                color = VayouTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

/**
 * An icon action for [VayouListHeader]'s right side, at the weight the label is drawn in.
 *
 * A bar button by default is a 24dp glyph in the full content colour, which beside a small grey
 * label reads as the loudest thing above the list -- and this one only says how the list is laid
 * out. Same size and same colour as the label makes the two ends of the line one control.
 */
@Composable
fun VayouListHeaderAction(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    VayouIconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VayouTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(VayouTheme.iconSize.sm),
        )
    }
}

private val OuterVertical = 4.dp

private val InnerVertical = 6.dp

private val ArrowSize = 16.dp
