package dev.vayou.settings.screens.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.ThemeConfig
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.PreferenceSwitch
import dev.vayou.core.ui.components.PreferenceSwitchWithDivider
import dev.vayou.core.ui.components.RadioTextButton
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.core.ui.designsystem.theme.supportsDynamicColors
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.settings.composables.OptionsDialog
import dev.vayou.settings.extensions.name
import androidx.compose.foundation.lazy.items

@Composable
fun AppearancePreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: AppearancePreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppearancePreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
private fun AppearancePreferencesContent(
    uiState: AppearancePreferencesUiState,
    onEvent: (AppearancePreferencesEvent) -> Unit,
    onNavigateUp: () -> Unit = {},
) {
    VayouScaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.appearance_name),
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            PreferenceGroup {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    description = uiState.preferences.themeConfig.name(),
                    isChecked = uiState.preferences.themeConfig == ThemeConfig.ON,
                    onChecked = { onEvent(AppearancePreferencesEvent.ToggleDarkTheme) },
                    icon = VayouIcons.DarkMode,
                    onClick = { onEvent(AppearancePreferencesEvent.ShowDialog(AppearancePreferenceDialog.Theme)) },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = VayouIcons.Contrast,
                    isChecked = uiState.preferences.useHighContrastDarkTheme,
                    onClick = { onEvent(AppearancePreferencesEvent.ToggleUseHighContrastDarkTheme) },
                )
                if (supportsDynamicColors()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_theme),
                        description = stringResource(id = R.string.dynamic_theme_description),
                        icon = VayouIcons.Appearance,
                        isChecked = uiState.preferences.useDynamicColors,
                        onClick = { onEvent(AppearancePreferencesEvent.ToggleUseDynamicColors) },
                    )
                }
            }
        }

        uiState.showDialog?.let { showDialog ->
            when (showDialog) {
                AppearancePreferenceDialog.Theme -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.dark_theme),
                        onDismissClick = { onEvent(AppearancePreferencesEvent.ShowDialog(null)) },
                    ) {
                        items(ThemeConfig.entries.toTypedArray(), key = { it.name }) {
                            RadioTextButton(
                                text = it.name(),
                                selected = (it == uiState.preferences.themeConfig),
                                onClick = {
                                    onEvent(AppearancePreferencesEvent.UpdateThemeConfig(it))
                                    onEvent(AppearancePreferencesEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AppearancePreferencesScreenPreview() {
    VayouPlayerTheme {
        AppearancePreferencesContent(
            uiState = AppearancePreferencesUiState(),
            onEvent = {},
        )
    }
}
