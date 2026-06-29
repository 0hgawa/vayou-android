package dev.vayou.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.BrowserSortBy

@Composable
fun BrowserSortMenu(
    sort: BrowserSort,
    onChange: (BrowserSort) -> Unit,
    onDismiss: () -> Unit,
) {
    Backdrop(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = "Ordenar por",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            BrowserSortBy.entries.forEachIndexed { index, axis ->
                SortRow(
                    axis = axis,
                    isSelected = sort.by == axis,
                    isAscending = sort.asc,
                    autoFocus = index == 0,
                    onClick = {
                        val next = if (sort.by == axis) sort.copy(asc = !sort.asc) else sort.copy(by = axis)
                        onChange(next)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun SortRow(
    axis: BrowserSortBy,
    isSelected: Boolean,
    isAscending: Boolean,
    autoFocus: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) runCatching { focus.requestFocus() } }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = cs.primaryContainer,
            contentColor = if (isSelected) cs.primary else cs.onSurface,
            focusedContentColor = cs.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = when (axis) {
                    BrowserSortBy.NAME -> Icons.Outlined.Title
                    BrowserSortBy.SIZE -> Icons.AutoMirrored.Outlined.CompareArrows
                },
                contentDescription = null,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
            )
            Text(
                text = axis.label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Icon(
                    imageVector = if (isAscending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(VayouTheme.iconSize.xs),
                )
            }
        }
    }
}
