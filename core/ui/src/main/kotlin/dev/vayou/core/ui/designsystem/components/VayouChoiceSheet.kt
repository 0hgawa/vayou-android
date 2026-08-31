package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults

/**
 * One value out of a named list.
 *
 * A sheet rather than a dialog, which is what the rest of this app opens for a choice -- the sort
 * order, the audio track, the speed. A dialog would be a second way of asking the same question.
 *
 * It closes on the pick. Confirming a single choice means two taps for one decision, and there is
 * nothing to weigh up: what was chosen is on the row behind the sheet the moment it is gone.
 */
@Composable
fun <T> VayouChoiceSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    description: (@Composable (T) -> String?)? = null,
) {
    VayouSheet(onDismissRequest = onDismiss) {
        VayouSheetTitle(text = title)
        LazyColumn(
            modifier = Modifier
                .heightIn(max = VayouSheetDefaults.ListMaxHeight)
                .selectableGroup(),
        ) {
            items(options) { option ->
                SingleSelectablePreference(
                    // On the sheet's own margin. Left to its default a preference sits on the
                    // 32dp inset a settings screen uses, which put every row eight dp inside the
                    // title above it.
                    contentPadding = MediaListLayoutDefaults.SheetItemPadding,
                    title = label(option),
                    description = description?.invoke(option),
                    selected = option == selected,
                    onClick = {
                        onPick(option)
                        onDismiss()
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}
