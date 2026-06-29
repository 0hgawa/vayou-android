package dev.vayou.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.vayou.core.ui.R
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun RenameDialog(
    name: String,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(name, TextRange(0, name.length))) }
    val focusRequester = remember { FocusRequester() }
    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_to)) },
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
            )
        },
        confirmButton = {
            DoneButton(
                enabled = value.text.isNotBlank() && value.text.trim() != name,
                onClick = { onDone(value.text.trim()) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
    LaunchedEffect(Unit) {
        delay(200.milliseconds)
        focusRequester.requestFocus()
    }
}
