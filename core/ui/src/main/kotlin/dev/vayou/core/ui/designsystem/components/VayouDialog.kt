package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.windowSize
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A question with an answer on either side of it.
 *
 * Sized here rather than by the platform, which caps a dialog at a width that leaves a tablet's
 * showing as a narrow column in the middle of the screen.
 */
@Composable
fun VayouDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = VayouDialogDefaults.Properties,
    content: @Composable () -> Unit,
) {
    val maxWidth = windowSize().width - VayouDialogDefaults.Margin * 2

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(max = maxWidth),
        title = { Text(text = title) },
        text = { Column { content() } },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        properties = properties,
        shape = VayouTheme.shapes.extraLarge,
        containerColor = VayouTheme.colors.surfaceContainerHigh,
    )
}

/**
 * The action the dialog exists to offer. Filled and inverted, so the pair reads as one strong
 * answer and one quiet one rather than two of the same weight.
 */
@Composable
fun VayouConfirmButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    VayouConfirmButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text = text) }
}

/**
 * The same button showing something other than a word -- a spinner, while the work it started is
 * still running. The colours live here so a button that waits cannot drift from the ones that do not.
 */
@Composable
fun VayouConfirmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    VayouButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = VayouTheme.colors.onSurface,
        contentColor = VayouTheme.colors.surface,
        content = content,
    )
}

/** Confirm, where the action needs no name beyond "that is what I meant". */
@Composable
fun VayouDoneButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    VayouConfirmButton(stringResource(R.string.done), onClick, modifier, enabled)
}

/** A plain text button: the dismissive action must never compete with the confirm. */
@Composable
fun VayouCancelButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    VayouTextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text = stringResource(R.string.cancel))
    }
}

object VayouDialogDefaults {
    val Properties = DialogProperties(usePlatformDefaultWidth = false)
    val Margin = 16.dp
}
