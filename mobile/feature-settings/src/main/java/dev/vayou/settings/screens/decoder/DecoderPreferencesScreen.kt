package dev.vayou.settings.screens.decoder

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.DecoderPriority
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.ClickablePreferenceItem
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.RadioTextButton
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.settings.composables.OptionsDialog
import dev.vayou.settings.extensions.name

@Composable
fun DecoderPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: DecoderPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DecoderPreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecoderPreferencesContent(
    uiState: DecoderPreferencesUiState,
    onEvent: (DecoderPreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val preferences = uiState.preferences

    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.decoder),
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
                    title = stringResource(R.string.decoder_priority),
                    description = preferences.decoderPriority.name(),
                    icon = VayouIcons.Priority,
                    onClick = { onEvent(DecoderPreferencesUiEvent.ShowDialog(DecoderPreferenceDialog.DecoderPriorityDialog)) },
                )
            }
        }

        uiState.showDialog?.let { showDialog ->
            when (showDialog) {
                DecoderPreferenceDialog.DecoderPriorityDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.decoder_priority),
                        onDismissClick = { onEvent(DecoderPreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(DecoderPriority.entries.toTypedArray(), key = { it.name }) {
                            RadioTextButton(
                                text = it.name(),
                                selected = it == preferences.decoderPriority,
                                onClick = {
                                    onEvent(DecoderPreferencesUiEvent.UpdateDecoderPriority(it))
                                    onEvent(DecoderPreferencesUiEvent.ShowDialog(null))
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
private fun DecoderPreferencesScreenPreview() {
    VayouPlayerTheme {
        DecoderPreferencesContent(
            uiState = DecoderPreferencesUiState(),
            onEvent = {},
            onNavigateUp = {},
        )
    }
}
