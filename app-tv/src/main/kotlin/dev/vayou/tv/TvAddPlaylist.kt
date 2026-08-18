package dev.vayou.tv

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource

/**
 * An address, and a name for it if the viewer can be bothered.
 *
 * The address is the whole of it -- a list with no name is called after the file it came from -- so
 * the name is asked for second and saving does not wait on it.
 */
@Composable
fun TvAddPlaylist(onAdd: (name: String, url: String) -> Unit, onDismiss: () -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    TvDialog(title = stringResource(R.string.add_playlist), onDismiss = onDismiss) {
        TvTextField(
            value = url,
            onValueChange = { url = it },
            label = stringResource(R.string.playlist_url),
            modifier = Modifier.focusRequester(first),
        )
        TvTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.playlist_name))
        Spacer(modifier = Modifier.height(TvRowGap))
        TvChoiceRow(label = stringResource(R.string.cancel), isSelected = false, onClick = onDismiss)
        // Absent until there is an address rather than greyed out: a D-pad walks past a disabled
        // row as if it were not there, so a dim one is a gap with a word in it.
        if (url.isNotBlank()) {
            TvChoiceRow(label = stringResource(R.string.save), isSelected = false) {
                onAdd(name.trim(), url.trim())
            }
        }
    }
}
