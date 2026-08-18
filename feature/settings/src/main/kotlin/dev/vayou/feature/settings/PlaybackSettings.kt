package dev.vayou.feature.settings

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.DecoderPriority
import dev.vayou.core.model.DefaultControllerTimeout
import dev.vayou.core.model.DefaultGestureSensitivity
import dev.vayou.core.model.DefaultSeekIncrement
import dev.vayou.core.model.DoubleTapGesture
import dev.vayou.core.model.Resume
import dev.vayou.core.model.ScreenOrientation
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.ListSectionTitle
import dev.vayou.core.ui.designsystem.components.PreferenceChoice
import dev.vayou.core.ui.designsystem.components.PreferenceGroup
import dev.vayou.core.ui.designsystem.components.PreferenceSlider
import dev.vayou.core.ui.designsystem.components.PreferenceSwitch
import dev.vayou.core.ui.designsystem.components.PreferenceSwitchWithDivider
import dev.vayou.core.ui.designsystem.components.VayouChoiceSheet
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import kotlin.math.roundToInt

/** How the player opens and behaves, as against what a touch on it does. */
@Composable
internal fun PlayerSettings(viewModel: SettingsViewModel, isPipSupported: Boolean) {
    val preferences by viewModel.player.collectAsStateWithLifecycle()
    var openSheet: PlaybackSheet? by remember { mutableStateOf(null) }

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_controls))
        PreferenceGroup {
            PreferenceSlider(
                title = stringResource(R.string.settings_controller_timeout),
                description = stringResource(R.string.settings_seconds, preferences.controllerAutoHideTimeout),
                icon = VayouIcons.Timer,
                value = preferences.controllerAutoHideTimeout.toFloat(),
                valueRange = MinControllerTimeout..MaxControllerTimeout,
                onValueChange = {
                    viewModel.updatePlayer { copy(controllerAutoHideTimeout = it.roundToInt()) }
                },
                trailingContent = {
                    ResetButton(
                        onClick = {
                            viewModel.updatePlayer { copy(controllerAutoHideTimeout = DefaultControllerTimeout) }
                        },
                    )
                },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_material_you),
                description = stringResource(R.string.settings_material_you_description),
                icon = VayouIcons.Appearance,
                isChecked = preferences.useMaterialYouControls,
                onClick = { viewModel.updatePlayer { copy(useMaterialYouControls = !useMaterialYouControls) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_ambient_glow),
                description = stringResource(R.string.settings_ambient_glow_description),
                icon = VayouIcons.Brightness,
                isChecked = preferences.useAmbientGlow,
                onClick = { viewModel.updatePlayer { copy(useAmbientGlow = !useAmbientGlow) } },
            )
        }

        ListSectionTitle(text = stringResource(R.string.settings_playback))
        PreferenceGroup {
            PreferenceChoice(
                title = stringResource(R.string.settings_resume),
                value = stringResource(preferences.resume.label),
                icon = VayouIcons.Resume,
                onClick = { openSheet = PlaybackSheet.Resume },
            )
            PreferenceChoice(
                title = stringResource(R.string.settings_orientation),
                value = stringResource(preferences.playerScreenOrientation.label),
                icon = VayouIcons.Rotation,
                onClick = { openSheet = PlaybackSheet.Orientation },
            )
            PreferenceSlider(
                title = stringResource(R.string.settings_default_speed),
                description = stringResource(R.string.settings_speed, preferences.defaultPlaybackSpeed),
                icon = VayouIcons.Speed,
                value = preferences.defaultPlaybackSpeed,
                valueRange = MinSpeed..MaxSpeed,
                // On release only. A speed written on every frame of the drag is a hundred writes
                // to disk for one decision.
                onValueChange = { viewModel.updatePlayer { copy(defaultPlaybackSpeed = it.toOneDecimal()) } },
                trailingContent = {
                    ResetButton(onClick = { viewModel.updatePlayer { copy(defaultPlaybackSpeed = NormalSpeed) } })
                },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_autoplay),
                description = stringResource(R.string.settings_autoplay_description),
                icon = VayouIcons.Play,
                isChecked = preferences.autoplay,
                onClick = { viewModel.updatePlayer { copy(autoplay = !autoplay) } },
            )
            // Hidden where the system has no floating window rather than shown doing nothing --
            // Android TV, and a handful of phones whose maker left the feature out.
            if (isPipSupported) {
                PreferenceSwitch(
                    title = stringResource(R.string.settings_pip),
                    description = stringResource(R.string.settings_pip_description),
                    icon = VayouIcons.Pip,
                    isChecked = preferences.autoPip,
                    onClick = { viewModel.updatePlayer { copy(autoPip = !autoPip) } },
                )
            }
            PreferenceSwitch(
                title = stringResource(R.string.settings_background_play),
                description = stringResource(R.string.settings_background_play_description),
                icon = VayouIcons.Headset,
                isChecked = preferences.autoBackgroundPlay,
                onClick = { viewModel.updatePlayer { copy(autoBackgroundPlay = !autoBackgroundPlay) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_remember_brightness),
                description = stringResource(R.string.settings_remember_brightness_description),
                icon = VayouIcons.Brightness,
                isChecked = preferences.rememberPlayerBrightness,
                onClick = { viewModel.updatePlayer { copy(rememberPlayerBrightness = !rememberPlayerBrightness) } },
            )
        }
    }

    when (openSheet) {
        null -> Unit
        PlaybackSheet.Resume -> VayouChoiceSheet(
            title = stringResource(R.string.settings_resume),
            options = Resume.entries,
            selected = preferences.resume,
            label = { stringResource(it.label) },
            onPick = { picked -> viewModel.updatePlayer { copy(resume = picked) } },
            onDismiss = { openSheet = null },
        )
        PlaybackSheet.Orientation -> VayouChoiceSheet(
            title = stringResource(R.string.settings_orientation),
            options = ScreenOrientation.entries,
            selected = preferences.playerScreenOrientation,
            label = { stringResource(it.label) },
            onPick = { picked -> viewModel.updatePlayer { copy(playerScreenOrientation = picked) } },
            onDismiss = { openSheet = null },
        )
    }
}

/** What a touch on the picture does. */
@Composable
internal fun GestureSettings(viewModel: SettingsViewModel) {
    val preferences by viewModel.player.collectAsStateWithLifecycle()
    var isDoubleTapSheetOpen by remember { mutableStateOf(false) }

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_swipe))
        PreferenceGroup {
            PreferenceSwitch(
                title = stringResource(R.string.settings_seek_gesture),
                description = stringResource(R.string.settings_seek_gesture_description),
                icon = VayouIcons.SwipeHorizontal,
                isChecked = preferences.useSeekControls,
                onClick = { viewModel.updatePlayer { copy(useSeekControls = !useSeekControls) } },
            )
            SensitivitySlider(
                value = preferences.seekSensitivity,
                enabled = preferences.useSeekControls,
                onValueChange = { viewModel.updatePlayer { copy(seekSensitivity = it) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_brightness_gesture),
                description = stringResource(R.string.settings_brightness_gesture_description),
                icon = VayouIcons.SwipeVertical,
                isChecked = preferences.enableBrightnessSwipeGesture,
                onClick = {
                    viewModel.updatePlayer { copy(enableBrightnessSwipeGesture = !enableBrightnessSwipeGesture) }
                },
            )
            SensitivitySlider(
                value = preferences.brightnessGestureSensitivity,
                enabled = preferences.enableBrightnessSwipeGesture,
                onValueChange = { viewModel.updatePlayer { copy(brightnessGestureSensitivity = it) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_volume_gesture),
                description = stringResource(R.string.settings_volume_gesture_description),
                icon = VayouIcons.SwipeVertical,
                isChecked = preferences.enableVolumeSwipeGesture,
                onClick = { viewModel.updatePlayer { copy(enableVolumeSwipeGesture = !enableVolumeSwipeGesture) } },
            )
            SensitivitySlider(
                value = preferences.volumeGestureSensitivity,
                enabled = preferences.enableVolumeSwipeGesture,
                onValueChange = { viewModel.updatePlayer { copy(volumeGestureSensitivity = it) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_volume_boost),
                description = stringResource(R.string.settings_volume_boost_description),
                icon = VayouIcons.VolumeUp,
                isChecked = preferences.enableVolumeBoost,
                onClick = { viewModel.updatePlayer { copy(enableVolumeBoost = !enableVolumeBoost) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_zoom_gesture),
                description = stringResource(R.string.settings_zoom_gesture_description),
                icon = VayouIcons.Pinch,
                isChecked = preferences.useZoomControls,
                onClick = { viewModel.updatePlayer { copy(useZoomControls = !useZoomControls) } },
            )
        }

        ListSectionTitle(text = stringResource(R.string.settings_tap))
        PreferenceGroup {
            PreferenceSwitchWithDivider(
                title = stringResource(R.string.settings_double_tap),
                description = stringResource(preferences.doubleTapGesture.label),
                icon = VayouIcons.DoubleTap,
                isChecked = preferences.doubleTapGesture != DoubleTapGesture.NONE,
                // Off is a value of the same setting, so the switch and the sheet are two ways to
                // the same place. Turning it back on returns what it was, not a default.
                onCheckedChange = {
                    viewModel.updatePlayer {
                        copy(
                            doubleTapGesture = if (doubleTapGesture == DoubleTapGesture.NONE) {
                                DoubleTapGesture.BOTH
                            } else {
                                DoubleTapGesture.NONE
                            },
                        )
                    }
                },
                onClick = { isDoubleTapSheetOpen = true },
            )
            PreferenceSlider(
                title = stringResource(R.string.settings_seek_increment),
                description = stringResource(R.string.settings_seconds, preferences.seekIncrement),
                icon = VayouIcons.Replay,
                enabled = preferences.doubleTapGesture != DoubleTapGesture.PLAY_PAUSE &&
                    preferences.doubleTapGesture != DoubleTapGesture.NONE,
                value = preferences.seekIncrement.toFloat(),
                valueRange = MinSeekIncrement..MaxSeekIncrement,
                onValueChange = { viewModel.updatePlayer { copy(seekIncrement = it.roundToInt()) } },
                trailingContent = {
                    ResetButton(onClick = { viewModel.updatePlayer { copy(seekIncrement = DefaultSeekIncrement) } })
                },
            )
        }
    }

    if (isDoubleTapSheetOpen) {
        VayouChoiceSheet(
            title = stringResource(R.string.settings_double_tap),
            options = DoubleTapGesture.entries,
            selected = preferences.doubleTapGesture,
            label = { stringResource(it.label) },
            onPick = { picked -> viewModel.updatePlayer { copy(doubleTapGesture = picked) } },
            onDismiss = { isDoubleTapSheetOpen = false },
        )
    }
}

/** Which decoder opens a file, which is the setting to reach for when one will not play. */
@Composable
internal fun DecoderSettings(viewModel: SettingsViewModel) {
    val preferences by viewModel.player.collectAsStateWithLifecycle()
    var isSheetOpen by remember { mutableStateOf(false) }

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_playback))
        PreferenceGroup {
            PreferenceChoice(
                title = stringResource(R.string.settings_decoder_priority),
                value = stringResource(preferences.decoderPriority.label),
                icon = VayouIcons.Priority,
                onClick = { isSheetOpen = true },
            )
        }
    }

    if (isSheetOpen) {
        VayouChoiceSheet(
            title = stringResource(R.string.settings_decoder_priority),
            options = DecoderPriority.entries,
            selected = preferences.decoderPriority,
            label = { stringResource(it.label) },
            description = { stringResource(it.description) },
            onPick = { picked -> viewModel.updatePlayer { copy(decoderPriority = picked) } },
            onDismiss = { isSheetOpen = false },
        )
    }
}

/** Who else on the phone gets a say in the sound. */
@Composable
internal fun AudioSettings(viewModel: SettingsViewModel) {
    val preferences by viewModel.player.collectAsStateWithLifecycle()

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_playback))
        PreferenceGroup {
            PreferenceSwitch(
                title = stringResource(R.string.settings_audio_focus),
                description = stringResource(R.string.settings_audio_focus_description),
                icon = VayouIcons.Focus,
                isChecked = preferences.requireAudioFocus,
                onClick = { viewModel.updatePlayer { copy(requireAudioFocus = !requireAudioFocus) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_headset),
                description = stringResource(R.string.settings_headset_description),
                icon = VayouIcons.Headset,
                isChecked = preferences.pauseOnHeadsetDisconnect,
                onClick = { viewModel.updatePlayer { copy(pauseOnHeadsetDisconnect = !pauseOnHeadsetDisconnect) } },
            )
        }
    }
}

@Composable
private fun SensitivitySlider(value: Float, enabled: Boolean, onValueChange: (Float) -> Unit) {
    PreferenceSlider(
        title = stringResource(R.string.settings_sensitivity),
        description = stringResource(R.string.settings_percent, (value * 100).roundToInt()),
        icon = VayouIcons.Sensitivity,
        enabled = enabled,
        value = value,
        valueRange = MinSensitivity..MaxSensitivity,
        onValueChange = onValueChange,
        trailingContent = {
            ResetButton(enabled = enabled, onClick = { onValueChange(DefaultGestureSensitivity) })
        },
    )
}

@Composable
private fun ResetButton(onClick: () -> Unit, enabled: Boolean = true) {
    VayouIconButton(enabled = enabled, onClick = onClick) {
        Icon(imageVector = VayouIcons.History, contentDescription = stringResource(R.string.settings_reset))
    }
}

private enum class PlaybackSheet { Resume, Orientation }

private val Resume.label: Int
    @StringRes get() = when (this) {
        Resume.YES -> R.string.settings_resume_yes
        Resume.NO -> R.string.settings_resume_no
    }

private val ScreenOrientation.label: Int
    @StringRes get() = when (this) {
        ScreenOrientation.AUTOMATIC -> R.string.settings_orientation_automatic
        ScreenOrientation.LANDSCAPE -> R.string.settings_orientation_landscape
        ScreenOrientation.LANDSCAPE_REVERSE -> R.string.settings_orientation_landscape_reverse
        ScreenOrientation.LANDSCAPE_AUTO -> R.string.settings_orientation_landscape_auto
        ScreenOrientation.PORTRAIT -> R.string.settings_orientation_portrait
        ScreenOrientation.VIDEO_ORIENTATION -> R.string.settings_orientation_video
    }

private val DoubleTapGesture.label: Int
    @StringRes get() = when (this) {
        DoubleTapGesture.SEEK -> R.string.settings_double_tap_seek
        DoubleTapGesture.PLAY_PAUSE -> R.string.settings_double_tap_play
        DoubleTapGesture.BOTH -> R.string.settings_double_tap_both
        DoubleTapGesture.NONE -> R.string.settings_double_tap_none
    }

private val DecoderPriority.label: Int
    @StringRes get() = when (this) {
        DecoderPriority.PREFER_DEVICE -> R.string.settings_decoder_device
        DecoderPriority.PREFER_APP -> R.string.settings_decoder_app
        DecoderPriority.DEVICE_ONLY -> R.string.settings_decoder_device_only
    }

private val DecoderPriority.description: Int
    @StringRes get() = when (this) {
        DecoderPriority.PREFER_DEVICE -> R.string.settings_decoder_device_description
        DecoderPriority.PREFER_APP -> R.string.settings_decoder_app_description
        DecoderPriority.DEVICE_ONLY -> R.string.settings_decoder_device_only_description
    }

private const val MinControllerTimeout = 1f

private const val MaxControllerTimeout = 30f

private const val MinSeekIncrement = 1f

private const val MaxSeekIncrement = 60f

/** A fifth of the speed, which is slow enough to read a frozen frame of text. */
private const val MinSpeed = 0.2f

/** Four times, past which speech is no longer speech. */
private const val MaxSpeed = 4f

private const val NormalSpeed = 1f

/** Never nothing: a sensitivity of zero is a gesture that is on and does not work. */
private const val MinSensitivity = 0.1f

private const val MaxSensitivity = 1f

private fun Float.toOneDecimal(): Float = (this * 10).roundToInt() / 10f
