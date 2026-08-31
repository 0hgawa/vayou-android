package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/** Naming something, whether it is being made or renamed. */
@Composable
fun VayouNameDialog(
    title: String,
    initialName: String,
    label: String,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    VayouDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = { VayouDoneButton(enabled = name.isNotBlank(), onClick = { onDone(name.trim()) }) },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        VayouTextField(
            value = name,
            onValueChange = { name = it },
            label = label,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
}

/** One choice in [VayouPickItemsSheet], flattened from whatever the caller's list holds. */
data class VayouPickItem(val key: String, val label: String, val supporting: String? = null)

/**
 * What to put in a list, picked from a library.
 *
 * What is already in the list is left out by the caller rather than shown ticked: the sheet is for
 * what is missing, and a page of mostly-disabled rows is harder to read than a shorter page of
 * choices.
 *
 * Takes flattened [VayouPickItem]s rather than a type of its own, because the two libraries that
 * open it hold different things and neither of them belongs in the design system.
 */
@Composable
fun VayouPickItemsSheet(
    title: String,
    items: List<VayouPickItem>,
    emptyIcon: ImageVector,
    emptyTitle: String,
    /** How the confirm row reads at [count] picked -- a plural the caller owns the wording of. */
    confirmLabel: @Composable (count: Int) -> String,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember { mutableStateOf(emptySet<String>()) }

    VayouSheet(onDismissRequest = onDismiss) {
        VayouSheetTitle(text = title)

        if (items.isEmpty()) {
            VayouEmptyState(emptyIcon, emptyTitle)
            Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
            return@VayouSheet
        }

        LazyColumn(modifier = Modifier.heightIn(max = VayouSheetDefaults.ListMaxHeight)) {
            items(items, key = { it.key }) { item ->
                val isPicked = item.key in picked
                VayouSegmentedListItem(
                    selected = isPicked,
                    contentPadding = MediaListLayoutDefaults.SheetItemPadding,
                    rippleColor = VayouTheme.colors.surfaceContainerHigh,
                    onClick = { picked = if (isPicked) picked - item.key else picked + item.key },
                    content = { Text(text = item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = item.supporting?.let {
                        {
                            Text(
                                text = it,
                                style = VayouTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                )
            }
        }

        // The same row it always was -- glyph, then the count -- moved to the trailing edge.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VayouSheetDefaults.HorizontalPadding),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        if (picked.isNotEmpty()) onConfirm(picked.toList())
                        onDismiss()
                    }
                    .padding(vertical = VayouTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
            ) {
                Icon(
                    imageVector = VayouIcons.Check,
                    contentDescription = null,
                    tint = VayouTheme.colors.onSurface,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
                Text(
                    text = confirmLabel(picked.size),
                    style = VayouTheme.typography.bodyLarge,
                    color = VayouTheme.colors.onSurface,
                )
            }
        }
        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}
