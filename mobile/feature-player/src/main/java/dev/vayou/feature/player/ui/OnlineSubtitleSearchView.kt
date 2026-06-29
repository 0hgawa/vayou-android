package dev.vayou.feature.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import dev.vayou.core.data.models.OpenSubtitleResult
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.player.OnlineSubtitleSearchState

private val LANGUAGES = listOf(
    "" to "All",
    "por" to "Português",
    "pob" to "Português (BR)",
    "eng" to "English",
    "spa" to "Español",
    "fre" to "Français",
    "ger" to "Deutsch",
    "ita" to "Italiano",
    "dut" to "Nederlands",
    "rus" to "Русский",
    "jpn" to "日本語",
    "chi" to "中文",
    "kor" to "한국어",
    "ara" to "العربية",
    "tur" to "Türkçe",
    "pol" to "Polski",
    "rum" to "Română",
    "hrv" to "Hrvatski",
    "scc" to "Srpski",
    "hun" to "Magyar",
    "cze" to "Čeština",
    "hin" to "हिन्दी",
)

@Composable
fun BoxScope.OnlineSubtitleSearchView(
    modifier: Modifier = Modifier,
    show: Boolean,
    state: OnlineSubtitleSearchState,
    onQueryChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSearch: () -> Unit,
    onResultClick: (OpenSubtitleResult, Int) -> Unit,
    onBack: () -> Unit,
) {
    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.search_online_subtitle),
        maxHeightFraction = 0.60f,
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = OverlayContentPadding)
                .padding(horizontal = OverlayContentPadding),
        ) {
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search),
                        color = VayouTheme.colors.onSurfaceVariant,
                    )
                },
                singleLine = true,
                textStyle = VayouTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = VayouTheme.colors.accent,
                    unfocusedIndicatorColor = VayouTheme.colors.outlineVariant,
                    cursorColor = VayouTheme.colors.accent,
                    focusedTextColor = VayouTheme.colors.onSurface,
                    unfocusedTextColor = VayouTheme.colors.onSurface,
                ),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        VayouIconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = VayouIcons.Close,
                                contentDescription = null,
                                modifier = Modifier.size(VayouTheme.iconSize.sm),
                            )
                        }
                    } else {
                        VayouIconButton(onClick = onSearch, enabled = !state.isLoading) {
                            Icon(
                                imageVector = VayouIcons.Search,
                                contentDescription = null,
                                modifier = Modifier.size(VayouTheme.iconSize.sm),
                            )
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            LanguageSelector(
                selectedLanguageId = state.languageId,
                onLanguageSelected = onLanguageChange,
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        VayouCircularProgress(size = 32.dp)
                    }
                }
                state.results.isNotEmpty() -> {
                    state.results.forEachIndexed { index, result ->
                        SubtitleResultRow(
                            result = result,
                            isDownloading = state.downloadingIndex == index,
                            onClick = { onResultClick(result, index) },
                        )
                    }
                }
                state.hasSearched && state.results.isEmpty() -> {
                    Text(
                        text = if (state.error == "no_results") stringResource(R.string.online_subtitle_no_results) else (state.error ?: stringResource(R.string.online_subtitle_no_results)),
                        style = VayouTheme.typography.bodyMedium,
                        color = VayouTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    selectedLanguageId: String,
    onLanguageSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = LANGUAGES.firstOrNull { it.first == selectedLanguageId }?.second ?: "All"

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = VayouIcons.Language,
                contentDescription = null,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
                tint = VayouTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedLabel,
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = VayouIcons.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
                tint = VayouTheme.colors.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column {
                LANGUAGES.forEach { (id, label) ->
                    val isSelected = id == selectedLanguageId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageSelected(id)
                                expanded = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = VayouIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(VayouTheme.iconSize.sm),
                                tint = VayouTheme.colors.accent,
                            )
                        } else {
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = VayouTheme.typography.bodyMedium,
                            color = VayouTheme.colors.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleResultRow(
    result: OpenSubtitleResult,
    isDownloading: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDownloading, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = result.subLanguageId.uppercase(),
            style = VayouTheme.typography.labelMedium,
            color = VayouTheme.colors.accent,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.subFileName,
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatDownloads(result.subDownloadsCnt),
                    style = VayouTheme.typography.bodySmall,
                    color = VayouTheme.colors.onSurfaceVariant,
                )
                if (result.matchedBy.isNotBlank()) {
                    Text(
                        text = if (result.matchedBy == "moviehash") "hash" else result.matchedBy,
                        style = VayouTheme.typography.bodySmall,
                        color = VayouTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        if (isDownloading) {
            VayouCircularProgress(size = 20.dp)
        }
    }
}

private fun formatDownloads(count: String): String {
    val num = count.toLongOrNull() ?: return count
    return when {
        num >= 1_000_000 -> "%.1fM".format(num / 1_000_000.0)
        num >= 1_000 -> "%.1fk".format(num / 1_000.0)
        else -> count
    } + " dl"
}
