package dev.vayou.settings.screens.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.CancelButton
import dev.vayou.core.ui.components.ClickablePreferenceItem
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons

@Composable
fun GeneralPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: GeneralPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GeneralPreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralPreferencesContent(
    uiState: GeneralPreferencesUiState,
    onEvent: (GeneralPreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.general_name),
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
            ListSectionTitle(text = stringResource(id = R.string.user_data))
            PreferenceGroup {
                ClickablePreferenceItem(
                    title = stringResource(R.string.delete_thumbnail_cache),
                    description = stringResource(R.string.delete_thumbnail_cache_description),
                    icon = VayouIcons.DeleteSweep,
                    onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.ClearThumbnailCacheDialog)) },
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.reset_settings),
                    description = stringResource(R.string.reset_settings_description),
                    icon = VayouIcons.History,
                    onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(GeneralPreferencesDialog.ResetSettingsDialog)) },
                )
            }
        }

        uiState.showDialog?.let { dialog ->
            when (dialog) {
                GeneralPreferencesDialog.ClearThumbnailCacheDialog -> {
                    VayouDialog(
                        onDismissRequest = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        title = {
                            Text(
                                text = stringResource(R.string.delete_thumbnail_cache),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onEvent(GeneralPreferencesUiEvent.ClearThumbnailCache)
                                    onEvent(GeneralPreferencesUiEvent.ShowDialog(null))
                                },
                            ) {
                                Text(text = stringResource(R.string.delete))
                            }
                        },
                        dismissButton = { CancelButton(onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) }) },
                        content = {
                            Text(
                                text = stringResource(R.string.delete_thumbnail_cache_confirmation),
                                style = VayouTheme.typography.titleSmall,
                            )
                        },
                    )
                }
                GeneralPreferencesDialog.ResetSettingsDialog -> {
                    VayouDialog(
                        onDismissRequest = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) },
                        title = {
                            Text(
                                text = stringResource(R.string.reset_settings),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onEvent(GeneralPreferencesUiEvent.ResetSettings)
                                    onEvent(GeneralPreferencesUiEvent.ShowDialog(null))
                                },
                            ) {
                                Text(text = stringResource(R.string.reset))
                            }
                        },
                        dismissButton = { CancelButton(onClick = { onEvent(GeneralPreferencesUiEvent.ShowDialog(null)) }) },
                        content = {
                            Text(
                                text = stringResource(R.string.reset_settings_confirmation),
                                style = VayouTheme.typography.titleSmall,
                            )
                        },
                    )
                }
            }
        }
    }
}
