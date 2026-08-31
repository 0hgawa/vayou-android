package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/** One axis a list can be ordered by. */
@Immutable
data class VayouSortOption(val label: String, val icon: ImageVector)

/**
 * Picks the order of a list. Shared, so that music and video cannot drift apart on either the rows
 * or the gesture.
 *
 * Tapping the axis already in use flips the direction rather than doing nothing. The arrow on that
 * row is both the current state and the control that reverses it, which is one tap where most
 * players carry a separate ascending and descending pair.
 */
@Composable
fun VayouSortSheet(
    title: String,
    options: List<VayouSortOption>,
    selectedIndex: Int,
    isAscending: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    VayouSheet(onDismissRequest = onDismiss) {
        VayouSheetTitle(text = title)
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            VayouSheetRow(
                text = option.label,
                onClick = { onSelect(index) },
                selected = isSelected,
                leading = { VayouSheetRowIcon(option.icon) },
                // The arrow below already says which is in force, so the row is not also recoloured:
                // a second signal carrying the same fact only stops the first from being trusted.
                trailing = if (isSelected) {
                    {
                        VayouSheetRowIcon(
                            icon = if (isAscending) VayouIcons.ArrowUpward else VayouIcons.ArrowDownward,
                            tint = VayouTheme.colors.accent,
                            isSmall = true,
                        )
                    }
                } else {
                    null
                },
            )
        }
        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}
