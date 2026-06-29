package dev.vayou.feature.player.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun RadioButtonRow(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
            )
            .padding(horizontal = OverlayContentPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = VayouIcons.Check,
                contentDescription = null,
                tint = VayouTheme.colors.accent,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = VayouTheme.typography.bodyMedium,
            color = VayouTheme.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
