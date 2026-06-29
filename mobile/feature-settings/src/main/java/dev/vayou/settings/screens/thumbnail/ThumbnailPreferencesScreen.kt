package dev.vayou.settings.screens.thumbnail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.ThumbnailGenerationStrategy
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.CancelButton
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.PreferenceSlider
import dev.vayou.core.ui.components.SingleSelectablePreference
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouPlayerTheme
import kotlin.math.abs

@Composable
fun ThumbnailPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: ThumbnailPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ThumbnailPreferencesContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThumbnailPreferencesContent(
    uiState: ThumbnailPreferencesUiState,
    onNavigateUp: () -> Unit,
    onEvent: (ThumbnailPreferencesEvent) -> Unit,
) {
    val preferences = uiState.preferences
    var frameSliderValue by rememberSaveable { mutableFloatStateOf(preferences.thumbnailFramePosition * 100f) }
    var pendingChange by remember { mutableStateOf<ThumbnailPreferenceChange?>(null) }

    LaunchedEffect(preferences.thumbnailFramePosition, pendingChange) {
        if (pendingChange == null) {
            frameSliderValue = preferences.thumbnailFramePosition * 100f
        }
    }

    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.thumbnail_generation),
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
            ListSectionTitle(text = stringResource(id = R.string.thumbnail_generation_strategy))
            PreferenceGroup(modifier = Modifier.selectableGroup()) {
                SingleSelectablePreference(
                    title = stringResource(id = R.string.first_frame),
                    description = stringResource(id = R.string.first_frame_desc),
                    selected = preferences.thumbnailGenerationStrategy == ThumbnailGenerationStrategy.FIRST_FRAME,
                    onClick = {
                        if (preferences.thumbnailGenerationStrategy == ThumbnailGenerationStrategy.FIRST_FRAME) return@SingleSelectablePreference
                        pendingChange = ThumbnailPreferenceChange.Strategy(ThumbnailGenerationStrategy.FIRST_FRAME)
                    },
                )
                SingleSelectablePreference(
                    title = stringResource(id = R.string.frame_at_position),
                    description = stringResource(id = R.string.frame_at_position_desc),
                    selected = preferences.thumbnailGenerationStrategy == ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE,
                    onClick = {
                        if (preferences.thumbnailGenerationStrategy == ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE) return@SingleSelectablePreference
                        pendingChange = ThumbnailPreferenceChange.Strategy(ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE)
                    },
                )
                SingleSelectablePreference(
                    title = stringResource(id = R.string.hybrid),
                    description = stringResource(id = R.string.hybrid_desc),
                    selected = preferences.thumbnailGenerationStrategy == ThumbnailGenerationStrategy.HYBRID,
                    onClick = {
                        if (preferences.thumbnailGenerationStrategy == ThumbnailGenerationStrategy.HYBRID) return@SingleSelectablePreference
                        pendingChange = ThumbnailPreferenceChange.Strategy(ThumbnailGenerationStrategy.HYBRID)
                    },
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.frame_position))
            PreferenceGroup {
                PreferenceSlider(
                    enabled = preferences.thumbnailGenerationStrategy != ThumbnailGenerationStrategy.FIRST_FRAME,
                    title = stringResource(R.string.frame_position),
                    description = stringResource(R.string.frame_position_value, frameSliderValue),
                    icon = VayouIcons.Frame,
                    value = frameSliderValue,
                    valueRange = 0f..100f,
                    onValueChange = { frameSliderValue = it },
                    onValueChangeFinished = {
                        val newPosition = frameSliderValue / 100f
                        if (abs(newPosition - preferences.thumbnailFramePosition) > 0.0001f) {
                            pendingChange = ThumbnailPreferenceChange.FramePosition(newPosition)
                        }
                    },
                    trailingContent = {
                        VayouIconButton(
                            enabled = preferences.thumbnailGenerationStrategy != ThumbnailGenerationStrategy.FIRST_FRAME,
                            onClick = {
                                val defaultPosition = ApplicationPreferences.DEFAULT_THUMBNAIL_FRAME_POSITION
                                if (abs(defaultPosition - preferences.thumbnailFramePosition) > 0.0001f) {
                                    frameSliderValue = defaultPosition * 100f
                                    pendingChange = ThumbnailPreferenceChange.FramePosition(defaultPosition)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = VayouIcons.History,
                                contentDescription = stringResource(id = R.string.reset_seek_sensitivity),
                            )
                        }
                    },
                )
            }
        }

        pendingChange?.let { change ->
            VayouDialog(
                onDismissRequest = {
                    pendingChange = null
                    frameSliderValue = preferences.thumbnailFramePosition * 100f
                },
                title = { Text(text = stringResource(id = R.string.thumbnail_generation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            when (change) {
                                is ThumbnailPreferenceChange.Strategy -> {
                                    onEvent(ThumbnailPreferencesEvent.UpdateStrategy(change.strategy))
                                }
                                is ThumbnailPreferenceChange.FramePosition -> {
                                    onEvent(ThumbnailPreferencesEvent.UpdateFramePosition(change.position))
                                }
                            }
                            pendingChange = null
                        },
                    ) {
                        Text(text = stringResource(id = R.string.okay))
                    }
                },
                dismissButton = {
                    CancelButton(
                        onClick = {
                            pendingChange = null
                            frameSliderValue = preferences.thumbnailFramePosition * 100f
                        },
                    )
                },
                content = {
                    Text(
                        text = stringResource(id = R.string.thumbnail_setting_change_confirmation),
                        style = VayouTheme.typography.titleSmall,
                    )
                },
            )
        }
    }
}

private sealed interface ThumbnailPreferenceChange {
    data class Strategy(val strategy: ThumbnailGenerationStrategy) : ThumbnailPreferenceChange
    data class FramePosition(val position: Float) : ThumbnailPreferenceChange
}

@PreviewLightDark
@Composable
private fun ThumbnailPreferencesScreenPreview() {
    VayouPlayerTheme {
        ThumbnailPreferencesContent(
            uiState = ThumbnailPreferencesUiState(),
            onNavigateUp = {},
            onEvent = {},
        )
    }
}
