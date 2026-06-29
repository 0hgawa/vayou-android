package dev.vayou.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.theme.VayouTheme

/** Modal with URL + optional name fields to add a custom M3U playlist. */
@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val urlFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { urlFocus.requestFocus() } }

    Backdrop(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(VayouTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
        ) {
            Text(
                text = "Adicionar playlist",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DialogTextField(
                label = "URL da playlist (M3U)",
                value = url,
                onValueChange = { url = it },
                focusRequester = urlFocus,
                imeAction = ImeAction.Next,
            )
            DialogTextField(
                label = "Nome (opcional)",
                value = name,
                onValueChange = { name = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm, Alignment.End),
            ) {
                VayouTvButton(onClick = onDismiss, label = "Cancelar")
                VayouTvButton(
                    onClick = {
                        if (url.isNotBlank()) onAdd(name.trim(), url.trim())
                    },
                    label = "Adicionar",
                    primary = true,
                    enabled = url.isNotBlank(),
                )
            }
        }
    }
}
