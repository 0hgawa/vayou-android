package dev.vayou.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.data.models.OnlineSubtitleState
import dev.vayou.core.data.models.OpenSubtitleResult
import dev.vayou.core.data.models.SubtitleLanguages
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouSheet
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouTextField
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Subtitles from OpenSubtitles, for a film that shipped without any.
 *
 * The search runs on opening without being asked, because by then the viewer has already said what
 * they want by pressing the button — and the first search is the one most likely to work, since it
 * goes by the file's fingerprint rather than its name. The field below is for when that misses.
 */
@Composable
fun OnlineSubtitleSheet(
    state: OnlineSubtitleState,
    onSearch: (query: String?, languageId: String) -> Unit,
    onPick: (OpenSubtitleResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var languageId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { onSearch(null, languageId) }

    val submit = { onSearch(query.trim().takeIf(String::isNotEmpty), languageId) }

    VayouSheet(onDismissRequest = onDismiss) {
        VayouSheetTitle(text = stringResource(R.string.online_subtitles))

        SearchField(
            query = query,
            onQueryChange = { query = it },
            onSubmit = submit,
        )

        LanguagePicker(
            selectedId = languageId,
            onSelect = {
                languageId = it
                // Searched again on the spot: the language is part of the question, and leaving
                // the previous answer on screen under a new one is a list that lies about itself.
                submit()
            },
        )

        when (state) {
            OnlineSubtitleState.Idle, OnlineSubtitleState.Searching -> Busy()
            OnlineSubtitleState.Failed -> Message(stringResource(R.string.online_subtitles_failed))
            is OnlineSubtitleState.Found -> Results(state.results, downloading = null, onPick = onPick)
            is OnlineSubtitleState.Downloading ->
                Results(state.results, downloading = state.downloading, onPick = onPick)
        }

        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}

/**
 * Which language to ask for, folded away until it is wanted.
 *
 * Shut by default because "every language" is the right first answer, and open it costs a dozen
 * rows above the results the viewer came to read.
 */
@Composable
private fun LanguagePicker(selectedId: String, onSelect: (String) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    val selected = SubtitleLanguages.firstOrNull { it.id == selectedId } ?: SubtitleLanguages.first()

    MenuRow(
        icon = VayouIcons.Language,
        text = selected.label,
        showChevron = true,
        onClick = { isOpen = !isOpen },
    )

    if (!isOpen) return
    SubtitleLanguages.forEach { language ->
        CheckedRow(
            text = language.label,
            isSelected = language.id == selectedId,
            onClick = {
                isOpen = false
                onSelect(language.id)
            },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    // The app's own field, not Material's boxed one. Every other place a name is typed -- naming a
    // list, adding a server -- draws a label above and a rule below, and a box on four sides here
    // was the one field in the app that looked like it came from somewhere else.
    VayouTextField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.online_subtitles_by_name),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        // Clear while there is something to clear, search while there is not: the two never
        // apply at once, and a box with no way out of it is a box you have to hold backspace on.
        trailing = {
            if (query.isEmpty()) {
                VayouIconButton(onClick = onSubmit) {
                    Icon(
                        imageVector = VayouIcons.Search,
                        contentDescription = stringResource(R.string.online_subtitles_search),
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                }
            } else {
                VayouIconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = VayouIcons.Close,
                        contentDescription = stringResource(R.string.clear),
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VayouSheetDefaults.HorizontalPadding),
    )
}

@Composable
private fun Results(
    results: List<OpenSubtitleResult>,
    downloading: OpenSubtitleResult?,
    onPick: (OpenSubtitleResult) -> Unit,
) {
    if (results.isEmpty()) {
        Message(stringResource(R.string.online_subtitles_none))
        return
    }
    Column(
        modifier = Modifier
            .heightIn(max = VayouSheetDefaults.ListMaxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        results.forEach { result ->
            ResultRow(
                result = result,
                isDownloading = result == downloading,
                // Nothing while one is already coming down: two at once would race to be the
                // selected track, and the loser would be a subtitle nobody asked for.
                onClick = { if (downloading == null) onPick(result) },
            )
        }
    }
}

@Composable
private fun ResultRow(result: OpenSubtitleResult, isDownloading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VayouSheetDefaults.HorizontalPadding, vertical = VayouTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.subFileName,
                style = VayouTheme.typography.bodyLarge,
                color = VayouTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // Language, and how many people took this one. On a title with forty results the
                // download count is the only thing separating a good sync from a bad one.
                text = "${result.subLanguageId} · ${result.subDownloadsCnt}",
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurfaceVariant,
            )
        }
        if (isDownloading) {
            VayouCircularProgress(size = SpinnerSize, strokeWidth = SpinnerStroke)
        }
    }
}

@Composable
private fun Busy() {
    Box(modifier = Modifier.fillMaxWidth().padding(MessagePadding), contentAlignment = Alignment.Center) {
        VayouCircularProgress(size = SpinnerSize, strokeWidth = SpinnerStroke)
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        style = VayouTheme.typography.bodyMedium,
        color = VayouTheme.colors.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(MessagePadding),
    )
}

private val MessagePadding = 24.dp

private val SpinnerSize = 24.dp

private val SpinnerStroke = 2.dp
