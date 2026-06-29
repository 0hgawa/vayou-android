package dev.vayou.settings.screens.gesture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.ui.designsystem.components.VayouSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.common.extensions.round
import dev.vayou.core.common.extensions.toString
import dev.vayou.core.model.DoubleTapGesture
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.VayouDialogWithDoneAndCancelButtons
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.PreferenceSlider
import dev.vayou.core.ui.components.PreferenceSwitch
import dev.vayou.core.ui.components.PreferenceSwitchWithDivider
import dev.vayou.core.ui.components.RadioTextButton
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.preview.DayNightPreview
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.settings.composables.OptionsDialog
import dev.vayou.settings.extensions.name

@Composable
fun GesturePreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: GesturePreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GesturePreferencesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GesturePreferencesContent(
    uiState: GesturePreferencesUiState,
    onEvent: (GesturePreferencesUiEvent) -> Unit,
    onNavigateUp: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.gestures),
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
            ListSectionTitle(text = stringResource(id = R.string.swipe_gestures))
            PreferenceGroup {
                PreferenceSwitch(
                    title = stringResource(id = R.string.seek_gesture),
                    description = stringResource(id = R.string.seek_gesture_description),
                    icon = VayouIcons.SwipeHorizontal,
                    isChecked = uiState.preferences.useSeekControls,
                    onClick = { onEvent(GesturePreferencesUiEvent.ToggleUseSeekControls) },
                )
                PreferenceSlider(
                    title = stringResource(R.string.seek_gesture_sensitivity),
                    description = uiState.preferences.seekSensitivity.toString(decimalPlaces = 2),
                    icon = VayouIcons.Sensitivity,
                    enabled = uiState.preferences.useSeekControls,
                    value = uiState.preferences.seekSensitivity,
                    valueRange = 0.1f..2.0f,
                    onValueChange = { onEvent(GesturePreferencesUiEvent.UpdateSeekSensitivity(it)) },
                    trailingContent = {
                        VayouIconButton(
                            enabled = uiState.preferences.useSeekControls,
                            onClick = { onEvent(GesturePreferencesUiEvent.UpdateSeekSensitivity(PlayerPreferences.DEFAULT_SEEK_SENSITIVITY)) },
                        ) {
                            Icon(
                                imageVector = VayouIcons.History,
                                contentDescription = stringResource(id = R.string.reset_seek_sensitivity),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.brightness_gesture),
                    description = stringResource(id = R.string.brightness_gesture_description),
                    icon = VayouIcons.SwipeVertical,
                    isChecked = uiState.preferences.enableBrightnessSwipeGesture,
                    onClick = { onEvent(GesturePreferencesUiEvent.ToggleEnableBrightnessSwipeGesture) },
                )
                PreferenceSlider(
                    title = stringResource(R.string.brightness_gesture_sensitivity),
                    description = uiState.preferences.brightnessGestureSensitivity.toString(decimalPlaces = 2),
                    icon = VayouIcons.Sensitivity,
                    enabled = uiState.preferences.enableBrightnessSwipeGesture,
                    value = uiState.preferences.brightnessGestureSensitivity,
                    valueRange = 0.1f..2.0f,
                    onValueChange = { onEvent(GesturePreferencesUiEvent.UpdateBrightnessGestureSensitivity(it)) },
                    trailingContent = {
                        VayouIconButton(
                            enabled = uiState.preferences.enableBrightnessSwipeGesture,
                            onClick = { onEvent(GesturePreferencesUiEvent.UpdateBrightnessGestureSensitivity(PlayerPreferences.DEFAULT_BRIGHTNESS_GESTURE_SENSITIVITY)) },
                        ) {
                            Icon(
                                imageVector = VayouIcons.History,
                                contentDescription = stringResource(id = R.string.reset_brightness_gesture_sensitivity),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.volume_gesture),
                    description = stringResource(id = R.string.volume_gesture_description),
                    icon = VayouIcons.SwipeVertical,
                    isChecked = uiState.preferences.enableVolumeSwipeGesture,
                    onClick = { onEvent(GesturePreferencesUiEvent.ToggleEnableVolumeSwipeGesture) },
                )
                PreferenceSlider(
                    title = stringResource(R.string.volume_gesture_sensitivity),
                    description = uiState.preferences.volumeGestureSensitivity.toString(decimalPlaces = 2),
                    icon = VayouIcons.Sensitivity,
                    enabled = uiState.preferences.enableVolumeSwipeGesture,
                    value = uiState.preferences.volumeGestureSensitivity,
                    valueRange = 0.1f..2.0f,
                    onValueChange = { onEvent(GesturePreferencesUiEvent.UpdateVolumeGestureSensitivity(it)) },
                    trailingContent = {
                        VayouIconButton(
                            enabled = uiState.preferences.enableVolumeSwipeGesture,
                            onClick = { onEvent(GesturePreferencesUiEvent.UpdateVolumeGestureSensitivity(PlayerPreferences.DEFAULT_VOLUME_GESTURE_SENSITIVITY)) },
                        ) {
                            Icon(
                                imageVector = VayouIcons.History,
                                contentDescription = stringResource(id = R.string.reset_volume_gesture_sensitivity),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.zoom_gesture),
                    description = stringResource(id = R.string.zoom_gesture_description),
                    icon = VayouIcons.Pinch,
                    isChecked = uiState.preferences.useZoomControls,
                    onClick = { onEvent(GesturePreferencesUiEvent.ToggleUseZoomControls) },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.pan_gesture),
                    description = stringResource(id = R.string.pan_gesture_description),
                    icon = VayouIcons.Pan,
                    enabled = uiState.preferences.useZoomControls,
                    isChecked = uiState.preferences.enablePanGesture,
                    onClick = { onEvent(GesturePreferencesUiEvent.ToggleEnablePanGesture) },
                )
            }
            ListSectionTitle(text = stringResource(id = R.string.tap_gestures))
            PreferenceGroup {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.double_tap),
                    description = stringResource(id = R.string.double_tap_description),
                    icon = VayouIcons.DoubleTap,
                    isChecked = (uiState.preferences.doubleTapGesture != DoubleTapGesture.NONE),
                    onChecked = { onEvent(GesturePreferencesUiEvent.ToggleDoubleTapGesture) },
                    onClick = { onEvent(GesturePreferencesUiEvent.ShowDialog(GesturePreferenceDialog.DoubleTapDialog)) },
                )
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.long_press_gesture),
                    description = stringResource(id = R.string.long_press_gesture_desc, uiState.preferences.longPressControlsSpeed),
                    icon = VayouIcons.Tap,
                    isChecked = uiState.preferences.useLongPressControls,
                    onChecked = { onEvent(GesturePreferencesUiEvent.ToggleUseLongPressControls) },
                    onClick = { onEvent(GesturePreferencesUiEvent.ShowDialog(GesturePreferenceDialog.LongPressControlsSpeedDialog)) },
                )
                PreferenceSlider(
                    title = stringResource(R.string.seek_increment),
                    description = stringResource(R.string.seconds, uiState.preferences.seekIncrement),
                    icon = VayouIcons.Replay,
                    value = uiState.preferences.seekIncrement.toFloat(),
                    valueRange = 1.0f..60.0f,
                    onValueChange = { onEvent(GesturePreferencesUiEvent.UpdateSeekIncrement(it.toInt())) },
                    trailingContent = {
                        VayouIconButton(onClick = { onEvent(GesturePreferencesUiEvent.UpdateSeekIncrement(PlayerPreferences.DEFAULT_SEEK_INCREMENT)) }) {
                            Icon(
                                imageVector = VayouIcons.History,
                                contentDescription = stringResource(id = R.string.reset_seek_increment),
                            )
                        }
                    },
                )
            }
        }

        uiState.showDialog?.let { showDialog ->
            when (showDialog) {
                GesturePreferenceDialog.DoubleTapDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.double_tap),
                        onDismissClick = { onEvent(GesturePreferencesUiEvent.ShowDialog(null)) },
                    ) {
                        items(DoubleTapGesture.entries.toTypedArray(), key = { it.name }) {
                            RadioTextButton(
                                text = it.name(),
                                selected = (it == uiState.preferences.doubleTapGesture),
                                onClick = {
                                    onEvent(GesturePreferencesUiEvent.UpdateDoubleTapGesture(it))
                                    onEvent(GesturePreferencesUiEvent.ShowDialog(null))
                                },
                            )
                        }
                    }
                }

                GesturePreferenceDialog.LongPressControlsSpeedDialog -> {
                    var longPressControlsSpeed by remember {
                        mutableFloatStateOf(uiState.preferences.longPressControlsSpeed)
                    }

                    VayouDialogWithDoneAndCancelButtons(
                        title = stringResource(R.string.long_press_gesture),
                        onDoneClick = {
                            onEvent(GesturePreferencesUiEvent.UpdateLongPressControlsSpeed(longPressControlsSpeed))
                            onEvent(GesturePreferencesUiEvent.ShowDialog(null))
                        },
                        onDismissClick = { onEvent(GesturePreferencesUiEvent.ShowDialog(null)) },
                        content = {
                            Text(
                                text = "$longPressControlsSpeed",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                textAlign = TextAlign.Center,
                                style = VayouTheme.typography.titleMedium,
                            )
                            VayouSlider(
                                value = longPressControlsSpeed,
                                onValueChange = { longPressControlsSpeed = it.round(1) },
                                valueRange = 0.2f..4.0f,
                            )
                        },
                    )
                }
            }
        }
    }
}

@DayNightPreview
@Composable
private fun GesturePreferencesScreenPreview() {
    VayouPlayerTheme {
        GesturePreferencesContent(
            uiState = GesturePreferencesUiState(),
            onEvent = {},
        )
    }
}
