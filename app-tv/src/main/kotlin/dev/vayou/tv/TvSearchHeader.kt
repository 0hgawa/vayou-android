package dev.vayou.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.VayouIcons

/**
 * Where the viewer is, the way back, and the one thing they can do from here.
 *
 * The box takes the place of the title rather than sitting beside it: a television header has room
 * for one line, and a box that is always open would take the focus on the way in and throw a
 * keyboard over the grid before anybody had asked for one.
 *
 * [query] is null while the viewer is browsing and a string, empty or not, while they are searching.
 * One field rather than a flag and a string: they cannot be searching for nothing.
 */
@Composable
fun TvSearchHeader(
    title: String?,
    query: String?,
    onSearch: (String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    /** One more mark beside the search one, for a screen with something else worth offering. */
    action: @Composable (() -> Unit)? = null,
) {
    val field = remember { FocusRequester() }
    LaunchedEffect(query != null) { if (query != null) runCatching { field.requestFocus() } }

    Row(
        modifier = modifier.padding(horizontal = TvScreenInset, vertical = TvRowInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvTitleInset),
    ) {
        if (query != null) {
            Box(modifier = Modifier.weight(1f)) {
                TvTextField(
                    value = query,
                    onValueChange = onSearch,
                    label = stringResource(R.string.search),
                    modifier = Modifier.focusRequester(field),
                )
            }
            return@Row
        }

        onBack?.let { TvBackButton(label = stringResource(R.string.back), onBack = it) }
        Text(
            text = title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
        TvIconButton(VayouIcons.Search, stringResource(R.string.search), onOpenSearch)
    }
}
