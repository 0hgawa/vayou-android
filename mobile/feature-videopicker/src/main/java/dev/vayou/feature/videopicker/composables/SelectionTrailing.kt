package dev.vayou.feature.videopicker.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.theme.VayouTheme

fun selectionTrailingFor(
    inSelectionMode: Boolean,
    selected: Boolean,
): (@Composable () -> Unit)? {
    if (!inSelectionMode || !selected) return null
    return {
        Icon(
            imageVector = VayouIcons.Check,
            contentDescription = null,
            tint = VayouTheme.colors.accent,
            modifier = Modifier.padding(8.dp),
        )
    }
}
