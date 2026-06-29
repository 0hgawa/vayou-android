package dev.vayou.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.theme.VayouTheme

/** Modal showing the source-picker entries triggered by the header's "Abrir". */
@Composable
fun OpenSourceMenu(
    onDismiss: () -> Unit,
    onOpenUrl: () -> Unit,
    onPickFile: () -> Unit,
) {
    Backdrop(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp),
        ) {
            MenuRow(
                icon = Icons.Outlined.Link,
                label = "Reproduzir URL",
                supporting = "Stream HTTP, HLS, RTSP…",
                autoFocus = true,
                onClick = onOpenUrl,
            )
            MenuRow(
                icon = Icons.Outlined.FolderOpen,
                label = "Procurar no dispositivo",
                supporting = "Vídeo do armazenamento local",
                autoFocus = false,
                onClick = onPickFile,
            )
        }
    }
}

/** Modal with a text field to play an arbitrary URL. */
@Composable
fun UrlInputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
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
                text = "Reproduzir URL",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DialogTextField(
                value = text,
                onValueChange = { text = it },
                focusRequester = fieldFocus,
                imeAction = ImeAction.Go,
                keyboardActions = KeyboardActions(onGo = { if (text.isNotBlank()) onConfirm(text.trim()) }),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm, Alignment.End),
            ) {
                VayouTvButton(onClick = onDismiss, label = "Cancelar")
                VayouTvButton(
                    onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                    label = "Reproduzir",
                    primary = true,
                    enabled = text.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    supporting: String,
    autoFocus: Boolean,
    onClick: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) runCatching { focus.requestFocus() }
    }
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(VayouTheme.iconSize.sm))
            Column {
                Text(text = label, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun Backdrop(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .onKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyDown) {
                    onDismiss(); true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}
