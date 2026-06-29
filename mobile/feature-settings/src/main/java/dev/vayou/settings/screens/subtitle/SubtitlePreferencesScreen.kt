package dev.vayou.settings.screens.subtitle

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.Font
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.ClickablePreferenceItem
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.PreferenceSlider
import dev.vayou.core.ui.components.PreferenceSwitch
import dev.vayou.core.ui.components.PreferenceSwitchWithDivider
import dev.vayou.core.ui.components.RadioTextButton
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.settings.composables.OptionsDialog
import dev.vayou.settings.extensions.name
import dev.vayou.settings.utils.LocalesHelper
import java.nio.charset.Charset

@Composable
fun SubtitlePreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: SubtitlePreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SubtitlePreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitlePreferencesContent(
    uiState: SubtitlePreferencesUiState,
    onEvent: (SubtitlePreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val languages = remember { listOf(Pair("None", "")) + LocalesHelper.getAvailableLocales() }
    val charsetResource = stringArrayResource(id = R.array.charsets_list)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.subtitle),
                navigationIcon = {
                    VayouIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = VayouIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = VayouTheme.colors.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding)
                .padding(bottom = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.playback))
            PreferenceGroup {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_subtitle_lang),
                    description = LocalesHelper.getLocaleDisplayLanguage(uiState.preferences.preferredSubtitleLanguage)
                        .takeIf { it.isNotBlank() } ?: stringResource(R.string.preferred_subtitle_lang_description),
                    icon = VayouIcons.Language,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleLanguageDialog)) },
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.subtitle_text_encoding),
                    description = charsetResource.first { it.contains(uiState.preferences.subtitleTextEncoding) },
                    icon = VayouIcons.Subtitle,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleEncodingDialog)) },
                )
            }
            ListSectionTitle(text = stringResource(id = R.string.visual))
            PreferenceGroup {
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.system_caption_style),
                    description = stringResource(R.string.system_caption_style_desc),
                    icon = VayouIcons.Caption,
                    isChecked = uiState.preferences.useSystemCaptionStyle,
                    onChecked = { onEvent(SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle) },
                    onClick = { context.startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS)) },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_font),
                    description = uiState.preferences.subtitleFont.name(),
                    icon = VayouIcons.Font,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(SubtitlePreferenceDialog.SubtitleFontDialog)) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_text_bold),
                    description = stringResource(id = R.string.subtitle_text_bold_desc),
                    icon = VayouIcons.Bold,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    isChecked = uiState.preferences.subtitleTextBold,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleSubtitleTextBold) },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_text_size),
                    description = uiState.preferences.subtitleTextSize.toString(),
                    icon = VayouIcons.FontSize,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    value = uiState.preferences.subtitleTextSize.toFloat(),
                    valueRange = 10f..60f,
                    onValueChange = { onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(it.toInt())) },
                    trailingContent = {
                        VayouIconButton(
                            enabled = uiState.preferences.useSystemCaptionStyle.not(),
                            onClick = {
                                onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE))
                            },
                        ) {
                            Icon(
                                imageVector = VayouIcons.History,
                                contentDescription = stringResource(id = R.string.reset_seek_increment),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_background),
                    description = stringResource(id = R.string.subtitle_background_desc),
                    icon = VayouIcons.Background,
                    enabled = uiState.preferences.useSystemCaptionStyle.not(),
                    isChecked = uiState.preferences.subtitleBackground,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleSubtitleBackground) },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.embedded_styles),
                    description = stringResource(R.string.embedded_styles_desc),
                    icon = VayouIcons.Style,
                    isChecked = uiState.preferences.applyEmbeddedStyles,
                    onClick = { onEvent(SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles) },
                )
            }
        }

        uiState.showDialog?.let { showDialog ->
            when (showDialog) {
                SubtitlePreferenceDialog.SubtitleLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_subtitle_lang),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(languages, key = { it.second }) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == uiState.preferences.preferredSubtitleLanguage,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleLanguage(it.second))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleFontDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_font),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(Font.entries.toTypedArray(), key = { it.name }) {
                            RadioTextButton(
                                text = it.name(),
                                selected = it == uiState.preferences.subtitleFont,
                                onClick = {
                                    onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleFont(it))
                                    onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleEncodingDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_text_encoding),
                        onDismissClick = { onEvent(SubtitlePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(charsetResource, key = { it }) {
                            val currentCharset = it.substringAfterLast("(", "").removeSuffix(")")
                            if (currentCharset.isEmpty() || Charset.isSupported(currentCharset)) {
                                RadioTextButton(
                                    text = it,
                                    selected = currentCharset == uiState.preferences.subtitleTextEncoding,
                                    onClick = {
                                        onEvent(SubtitlePreferencesUiEvent.UpdateSubtitleEncoding(currentCharset))
                                        onEvent(SubtitlePreferencesUiEvent.ShowDialog(null))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SubtitlePreferencesScreenPreview() {
    VayouPlayerTheme {
        SubtitlePreferencesContent(
            uiState = SubtitlePreferencesUiState(),
            onEvent = {},
            onNavigateUp = {},
        )
    }
}
