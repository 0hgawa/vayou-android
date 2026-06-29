package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.defaultMinSize(minWidth = 200.dp),
        shape = VayouTheme.shapes.medium,
        containerColor = VayouTheme.colors.surfaceContainerHigh,
        content = { content() },
    )
}

@Composable
fun VayouDropdownMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = VayouTheme.colors.onSurface,
) {
    DropdownMenuItem(
        modifier = modifier.fillMaxWidth(),
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = text,
                    style = VayouTheme.typography.labelLarge,
                    color = contentColor,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        },
        onClick = onClick,
    )
}
