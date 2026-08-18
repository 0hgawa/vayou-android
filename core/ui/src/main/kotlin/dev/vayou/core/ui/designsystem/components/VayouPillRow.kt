package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The row of pills that sits under a section's title and picks how its library is sliced.
 *
 * Scrolls sideways so the set can grow past the screen without wrapping into a block — a second row
 * of pills costs more vertical space than the list below it can spare. Selection is by index because
 * every caller drives it from an enum's entries.
 */
@Composable
fun VayouPillRow(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            VayouFilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = label,
            )
        }
    }
}
