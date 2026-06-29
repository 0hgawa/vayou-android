package dev.vayou.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.theme.VayouTheme

data class ContextAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/** Modal context menu (long-press / Menu key) listing actions for a focused item. */
@Composable
fun ItemContextMenu(
    title: String,
    actions: List<ContextAction>,
    onDismiss: () -> Unit,
) {
    val armed = remember { booleanArrayOf(false) }
    Backdrop(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp)
                .onPreviewKeyEvent { event ->
                    if (!armed[0] &&
                        event.type == KeyEventType.KeyUp &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                    ) {
                        armed[0] = true
                        true
                    } else false
                },
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            actions.forEachIndexed { index, action ->
                ActionRow(
                    icon = action.icon,
                    label = action.label,
                    autoFocus = index == 0,
                    onClick = {
                        action.onClick()
                        onDismiss()
                    },
                )
            }
        }
    }
}

/** Modal with a text input pre-filled with the current name; confirm renames. */
@Composable
fun RenameDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }

    Backdrop(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(VayouTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DialogTextField(
                value = text,
                onValueChange = { text = it },
                focusRequester = fieldFocus,
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text.trim()) }),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.xs, Alignment.End),
            ) {
                VayouTvButton(onClick = onDismiss, label = "Cancelar")
                VayouTvButton(
                    onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                    label = "Salvar",
                    primary = true,
                    enabled = text.isNotBlank() && text.trim() != initialValue,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    autoFocus: Boolean,
    onClick: () -> Unit,
) {
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
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(VayouTheme.iconSize.sm))
            Text(text = label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
