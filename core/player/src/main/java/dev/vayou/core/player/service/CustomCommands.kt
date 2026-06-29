package dev.vayou.core.player.service

import android.net.Uri
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import dev.vayou.core.model.EqPreset
import kotlinx.coroutines.guava.await

enum class CustomCommands(val customAction: String) {
    ADD_SUBTITLE_TRACK(customAction = "ADD_SUBTITLE_TRACK"),
    SET_SKIP_SILENCE_ENABLED(customAction = "SET_SKIP_SILENCE_ENABLED"),
    GET_SKIP_SILENCE_ENABLED(customAction = "GET_SKIP_SILENCE_ENABLED"),
    SET_IS_SCRUBBING_MODE_ENABLED(customAction = "SET_IS_SCRUBBING_MODE_ENABLED"),
    GET_SUBTITLE_DELAY(customAction = "GET_SUBTITLE_DELAY"),
    SET_SUBTITLE_DELAY(customAction = "SET_SUBTITLE_DELAY"),
    GET_SUBTITLE_SPEED(customAction = "GET_SUBTITLE_SPEED"),
    SET_SUBTITLE_SPEED(customAction = "SET_SUBTITLE_SPEED"),
    STOP_PLAYER_SESSION(customAction = "STOP_PLAYER_SESSION"),
    IS_LOUDNESS_GAIN_SUPPORTED(customAction = "IS_LOUDNESS_GAIN_SUPPORTED"),
    SET_LOUDNESS_GAIN(customAction = "SET_LOUDNESS_GAIN"),
    GET_LOUDNESS_GAIN(customAction = "GET_LOUDNESS_GAIN"),
    GET_EQUALIZER_BANDS(customAction = "GET_EQUALIZER_BANDS"),
    SET_EQUALIZER_ENABLED(customAction = "SET_EQUALIZER_ENABLED"),
    SET_EQUALIZER_BAND_LEVEL(customAction = "SET_EQUALIZER_BAND_LEVEL"),
    APPLY_EQUALIZER_PRESET(customAction = "APPLY_EQUALIZER_PRESET"),
    SET_NIGHT_MODE_ENABLED(customAction = "SET_NIGHT_MODE_ENABLED"),
    ;

    val sessionCommand = SessionCommand(customAction, Bundle.EMPTY)

    companion object {
        fun fromSessionCommand(sessionCommand: SessionCommand): CustomCommands? {
            return entries.find { it.customAction == sessionCommand.customAction }
        }

        fun asSessionCommands(): List<SessionCommand> {
            return entries.map { it.sessionCommand }
        }

        const val SUBTITLE_TRACK_URI_KEY = "subtitle_track_uri"
        const val SKIP_SILENCE_ENABLED_KEY = "skip_silence_enabled"
        const val IS_SCRUBBING_MODE_ENABLED_KEY = "is_scrubbing_mode_enabled"
        const val SUBTITLE_DELAY_KEY = "subtitle_delay"
        const val SUBTITLE_SPEED_KEY = "subtitle_speed"
        const val LOUDNESS_GAIN_KEY = "loudness_gain"
        const val IS_LOUDNESS_GAIN_SUPPORTED_KEY = "is_loudness_gain_supported"
        const val EQ_ENABLED_KEY = "eq_enabled"
        const val EQ_BAND_INDEX_KEY = "eq_band_index"
        const val EQ_BAND_LEVEL_KEY = "eq_band_level"
        const val EQ_PRESET_KEY = "eq_preset"
        const val EQ_BAND_COUNT_KEY = "eq_band_count"
        const val EQ_BAND_INDICES_KEY = "eq_band_indices"
        const val EQ_BAND_CENTER_FREQS_KEY = "eq_band_center_freqs"
        const val EQ_BAND_LEVELS_KEY = "eq_band_levels"
        const val EQ_BAND_MIN_KEY = "eq_band_min"
        const val EQ_BAND_MAX_KEY = "eq_band_max"
        const val NIGHT_MODE_ENABLED_KEY = "night_mode_enabled"
    }
}

fun MediaController.addSubtitleTrack(uri: Uri) {
    val args = Bundle().apply {
        putString(CustomCommands.SUBTITLE_TRACK_URI_KEY, uri.toString())
    }
    sendCustomCommand(CustomCommands.ADD_SUBTITLE_TRACK.sessionCommand, args)
}

suspend fun MediaController.setSkipSilenceEnabled(enabled: Boolean) {
    val args = Bundle().apply {
        putBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY, enabled)
    }
    sendCustomCommand(CustomCommands.SET_SKIP_SILENCE_ENABLED.sessionCommand, args).await()
}

fun MediaController.setMediaControllerIsScrubbingModeEnabled(enabled: Boolean) {
    val args = Bundle().apply {
        putBoolean(CustomCommands.IS_SCRUBBING_MODE_ENABLED_KEY, enabled)
    }
    sendCustomCommand(CustomCommands.SET_IS_SCRUBBING_MODE_ENABLED.sessionCommand, args)
}

suspend fun MediaController.getSkipSilenceEnabled(): Boolean {
    val result = sendCustomCommand(CustomCommands.GET_SKIP_SILENCE_ENABLED.sessionCommand, Bundle.EMPTY)
    return result.await().extras.getBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY, false)
}

fun MediaController.setSubtitleDelayMilliseconds(delayMillis: Long) {
    val args = Bundle().apply {
        putLong(CustomCommands.SUBTITLE_DELAY_KEY, delayMillis)
    }
    sendCustomCommand(CustomCommands.SET_SUBTITLE_DELAY.sessionCommand, args)
}

suspend fun MediaController.getSubtitleDelayMilliseconds(): Long {
    val result = sendCustomCommand(CustomCommands.GET_SUBTITLE_DELAY.sessionCommand, Bundle.EMPTY)
    return result.await().extras.getLong(CustomCommands.SUBTITLE_DELAY_KEY, 0L)
}

fun MediaController.setSubtitleSpeed(speed: Float) {
    val args = Bundle().apply {
        putFloat(CustomCommands.SUBTITLE_SPEED_KEY, speed)
    }
    sendCustomCommand(CustomCommands.SET_SUBTITLE_SPEED.sessionCommand, args)
}

suspend fun MediaController.getSubtitleSpeed(): Float {
    val result = sendCustomCommand(CustomCommands.GET_SUBTITLE_SPEED.sessionCommand, Bundle.EMPTY)
    return result.await().extras.getFloat(CustomCommands.SUBTITLE_SPEED_KEY, 1f)
}

fun MediaController.stopPlayerSession() {
    sendCustomCommand(CustomCommands.STOP_PLAYER_SESSION.sessionCommand, Bundle.EMPTY)
}

fun MediaController.setLoudnessGain(gain: Int) {
    val args = Bundle().apply {
        putInt(CustomCommands.LOUDNESS_GAIN_KEY, gain)
    }
    sendCustomCommand(CustomCommands.SET_LOUDNESS_GAIN.sessionCommand, args)
}

suspend fun MediaController.getLoudnessGain(): Int {
    val result = sendCustomCommand(CustomCommands.GET_LOUDNESS_GAIN.sessionCommand, Bundle.EMPTY)
    return result.await().extras.getInt(CustomCommands.LOUDNESS_GAIN_KEY, 0)
}

suspend fun MediaController.getIsLoudnessGainSupported(): Boolean {
    val result = sendCustomCommand(CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED.sessionCommand, Bundle.EMPTY)
    return result.await().extras.getBoolean(CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED_KEY, false)
}

suspend fun MediaController.getEqualizerBands(): Pair<List<Triple<Short, Int, Short>>, Pair<Short, Short>>? {
    val result = sendCustomCommand(CustomCommands.GET_EQUALIZER_BANDS.sessionCommand, Bundle.EMPTY).await()
    val extras = result.extras
    val count = extras.getInt(CustomCommands.EQ_BAND_COUNT_KEY, 0)
    if (count == 0) return null
    val indices = extras.getIntArray(CustomCommands.EQ_BAND_INDICES_KEY) ?: return null
    val centerFreqs = extras.getIntArray(CustomCommands.EQ_BAND_CENTER_FREQS_KEY) ?: return null
    val levels = extras.getIntArray(CustomCommands.EQ_BAND_LEVELS_KEY) ?: return null
    val min = extras.getInt(CustomCommands.EQ_BAND_MIN_KEY).toShort()
    val max = extras.getInt(CustomCommands.EQ_BAND_MAX_KEY).toShort()
    val bands = indices.mapIndexed { i, index ->
        Triple(index.toShort(), centerFreqs[i], levels[i].toShort())
    }
    return Pair(bands, Pair(min, max))
}

fun MediaController.setEqualizerEnabled(enabled: Boolean) {
    val args = Bundle().apply { putBoolean(CustomCommands.EQ_ENABLED_KEY, enabled) }
    sendCustomCommand(CustomCommands.SET_EQUALIZER_ENABLED.sessionCommand, args)
}

fun MediaController.setEqualizerBandLevel(index: Short, level: Short) {
    val args = Bundle().apply {
        putInt(CustomCommands.EQ_BAND_INDEX_KEY, index.toInt())
        putInt(CustomCommands.EQ_BAND_LEVEL_KEY, level.toInt())
    }
    sendCustomCommand(CustomCommands.SET_EQUALIZER_BAND_LEVEL.sessionCommand, args)
}

fun MediaController.applyEqualizerPreset(preset: EqPreset) {
    val args = Bundle().apply { putString(CustomCommands.EQ_PRESET_KEY, preset.name) }
    sendCustomCommand(CustomCommands.APPLY_EQUALIZER_PRESET.sessionCommand, args)
}

fun MediaController.setNightModeEnabled(enabled: Boolean) {
    val args = Bundle().apply { putBoolean(CustomCommands.NIGHT_MODE_ENABLED_KEY, enabled) }
    sendCustomCommand(CustomCommands.SET_NIGHT_MODE_ENABLED.sessionCommand, args)
}
