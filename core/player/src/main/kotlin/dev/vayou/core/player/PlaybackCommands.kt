package dev.vayou.core.player

import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import dev.vayou.core.model.AudioEffectType
import dev.vayou.core.model.EqPreset
import kotlinx.coroutines.guava.await

/**
 * What a screen can ask the service beyond starting and stopping.
 *
 * A session speaks a fixed vocabulary — play, pause, seek — and anything past it travels as a
 * custom command. The alternative is binding to the service directly, which gives back a reference
 * that has to be released on exactly the right lifecycle callback.
 */
object PlaybackCommands {

    val All: List<SessionCommand> = listOf(
        SetSleepTimer,
        GetSleepTimer,
        GetEqualizerBands,
        SetEqualizerEnabled,
        SetEqualizerBandLevel,
        ApplyEqualizerPreset,
        GetSupportedAudioEffects,
        SetAudioEffectStrength,
        SetNightMode,
        SetScrubbing,
        SetSubtitleDelay,
        SetVolumeBoost,
        GetVolumeBoostSupport,
        SetSkipSilence,
        GetSkipSilence,
    ).map { SessionCommand(it, Bundle.EMPTY) }

    const val SetSleepTimer = "SET_SLEEP_TIMER"
    const val GetSleepTimer = "GET_SLEEP_TIMER"

    const val MinutesKey = "sleep_timer_minutes"
    const val DeadlineKey = "sleep_timer_deadline"

    const val GetEqualizerBands = "GET_EQUALIZER_BANDS"
    const val SetEqualizerEnabled = "SET_EQUALIZER_ENABLED"
    const val SetEqualizerBandLevel = "SET_EQUALIZER_BAND_LEVEL"
    const val ApplyEqualizerPreset = "APPLY_EQUALIZER_PRESET"
    const val GetSupportedAudioEffects = "GET_SUPPORTED_AUDIO_EFFECTS"
    const val SetAudioEffectStrength = "SET_AUDIO_EFFECT_STRENGTH"

    const val SetNightMode = "SET_NIGHT_MODE"

    const val SetScrubbing = "SET_SCRUBBING"

    const val SetSubtitleDelay = "SET_SUBTITLE_DELAY"

    const val SetVolumeBoost = "SET_VOLUME_BOOST"
    const val GetVolumeBoostSupport = "GET_VOLUME_BOOST_SUPPORT"

    const val SetSkipSilence = "SET_SKIP_SILENCE"
    const val GetSkipSilence = "GET_SKIP_SILENCE"

    const val ScrubbingKey = "scrubbing"

    const val SubtitleDelayKey = "subtitle_delay"

    const val BoostKey = "volume_boost"
    const val BoostSupportedKey = "volume_boost_supported"

    const val SkipSilenceKey = "skip_silence"

    const val NightModeKey = "night_mode_enabled"

    const val EnabledKey = "eq_enabled"
    const val BandKey = "eq_band"
    const val LevelKey = "eq_level"
    const val PresetKey = "eq_preset"
    const val CentreFreqsKey = "eq_centre_freqs"
    const val LevelsKey = "eq_levels"
    const val MinLevelKey = "eq_min_level"
    const val MaxLevelKey = "eq_max_level"
    const val EffectsKey = "audio_effects"
    const val EffectKey = "audio_effect"
    const val StrengthKey = "audio_effect_strength"

    /** Nothing armed. */
    const val Off = 0

    /** Stop when what is playing now ends, which has no hour to count towards. */
    const val EndOfTrack = -1
}

/** How a timer stands: the choice that armed it, and when it runs out. */
data class SleepTimer(val minutes: Int, val deadlineMs: Long) {
    /** Null while nothing is counting — off, or waiting on a track rather than on the clock. */
    val remainingMs: Long?
        get() = (deadlineMs - SystemClock.elapsedRealtime()).takeIf { deadlineMs > 0L && it > 0L }

    val isArmed: Boolean get() = minutes != PlaybackCommands.Off
}

suspend fun MediaController.setSleepTimer(minutes: Int) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetSleepTimer, Bundle.EMPTY),
        bundleOf(PlaybackCommands.MinutesKey to minutes),
    ).await()
}

suspend fun MediaController.sleepTimer(): SleepTimer {
    val result = sendCustomCommand(
        SessionCommand(PlaybackCommands.GetSleepTimer, Bundle.EMPTY),
        Bundle.EMPTY,
    ).await()
    return SleepTimer(
        minutes = result.extras.getInt(PlaybackCommands.MinutesKey, PlaybackCommands.Off),
        deadlineMs = result.extras.getLong(PlaybackCommands.DeadlineKey, 0L),
    )
}

/** One band of the equalizer, as the screen draws it. */
data class EqualizerBand(
    val index: Short,
    val centreFreqHz: Int,
    val levelMillibels: Int,
    val minMillibels: Int,
    val maxMillibels: Int,
)

/** Null while nothing is attached to the audio session, which is what "no equalizer" looks like. */
suspend fun MediaController.equalizerBands(): List<EqualizerBand>? {
    val extras = sendCustomCommand(
        SessionCommand(PlaybackCommands.GetEqualizerBands, Bundle.EMPTY),
        Bundle.EMPTY,
    ).await().extras
    val centreFreqs = extras.getIntArray(PlaybackCommands.CentreFreqsKey) ?: return null
    val levels = extras.getIntArray(PlaybackCommands.LevelsKey) ?: return null
    val min = extras.getInt(PlaybackCommands.MinLevelKey)
    val max = extras.getInt(PlaybackCommands.MaxLevelKey)
    return centreFreqs.mapIndexed { band, centreFreqHz ->
        EqualizerBand(band.toShort(), centreFreqHz, levels[band], min, max)
    }
}

fun MediaController.setEqualizerEnabled(enabled: Boolean) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetEqualizerEnabled, Bundle.EMPTY),
        Bundle().apply { putBoolean(PlaybackCommands.EnabledKey, enabled) },
    )
}

fun MediaController.setEqualizerBandLevel(band: Short, millibels: Int) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetEqualizerBandLevel, Bundle.EMPTY),
        bundleOf(PlaybackCommands.BandKey to band.toInt(), PlaybackCommands.LevelKey to millibels),
    )
}

fun MediaController.applyEqualizerPreset(preset: EqPreset) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.ApplyEqualizerPreset, Bundle.EMPTY),
        Bundle().apply { putString(PlaybackCommands.PresetKey, preset.name) },
    )
}

/**
 * Asked for when the equalizer is opened rather than on connection: answering it attaches the
 * effects to the audio session, which is wasted work for anyone who never looks.
 */
suspend fun MediaController.supportedAudioEffects(): Set<AudioEffectType> {
    val names = sendCustomCommand(
        SessionCommand(PlaybackCommands.GetSupportedAudioEffects, Bundle.EMPTY),
        Bundle.EMPTY,
    ).await().extras.getStringArray(PlaybackCommands.EffectsKey).orEmpty()
    return names.mapNotNullTo(mutableSetOf()) { name ->
        AudioEffectType.entries.find { it.name == name }
    }
}

fun MediaController.setAudioEffectStrength(type: AudioEffectType, strength: Int) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetAudioEffectStrength, Bundle.EMPTY),
        Bundle().apply {
            putString(PlaybackCommands.EffectKey, type.name)
            putInt(PlaybackCommands.StrengthKey, strength)
        },
    )
}

private fun bundleOf(vararg pairs: Pair<String, Int>) = Bundle().apply {
    pairs.forEach { (key, value) -> putInt(key, value) }
}

/** Turn the night-mode limiter on or off on the session that is playing. */
fun MediaController.setNightMode(enabled: Boolean) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetNightMode, Bundle.EMPTY),
        Bundle().apply { putBoolean(PlaybackCommands.NightModeKey, enabled) },
    )
}

/**
 * Tell the player a finger is on the seek bar.
 *
 * While it is, the player seeks to the nearest keyframe rather than to the exact millisecond, and
 * skips the work it would otherwise do to settle after each one. A drag across a phone is a hundred
 * seeks a second, and done exactly each of those is a decode from the previous keyframe forward --
 * the picture stops following the finger and the phone gets hot.
 */
fun MediaController.setScrubbing(isScrubbing: Boolean) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetScrubbing, Bundle.EMPTY),
        Bundle().apply { putBoolean(PlaybackCommands.ScrubbingKey, isScrubbing) },
    )
}

/**
 * How far ahead of the sound the words are read out, in milliseconds.
 *
 * Sent rather than set: what the offset actually moves is a renderer, and the renderer lives in the
 * service. Negative brings the caption forward, positive holds it back.
 *
 * It is a property of the renderer and not of the film, so it stays put across a change of item --
 * which is why the caller re-sends it, zero included, whenever a new file opens.
 */
fun MediaController.setSubtitleDelay(millis: Long) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetSubtitleDelay, Bundle.EMPTY),
        Bundle().apply { putLong(PlaybackCommands.SubtitleDelayKey, millis) },
    )
}

/**
 * Amplify past what the device gives, in millibels.
 *
 * Sent on every step of the gesture that crosses the top of the range, which is why the caller is
 * expected to send only when the number actually changes: below the top there is nothing to say.
 */
fun MediaController.setVolumeBoost(millibels: Int) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetVolumeBoost, Bundle.EMPTY),
        Bundle().apply { putInt(PlaybackCommands.BoostKey, millibels) },
    )
}

/** Whether this device will amplify at all. Asked once, when the screen binds. */
suspend fun MediaController.isVolumeBoostSupported(): Boolean =
    sendCustomCommand(SessionCommand(PlaybackCommands.GetVolumeBoostSupport, Bundle.EMPTY), Bundle.EMPTY)
        .await()
        .extras
        .getBoolean(PlaybackCommands.BoostSupportedKey)

/**
 * Run the quiet stretches together rather than playing them.
 *
 * A property of the player rather than of the film, and not written down anywhere: it belongs to
 * this run of listening the way the playback speed does.
 */
fun MediaController.setSkipSilence(enabled: Boolean) {
    sendCustomCommand(
        SessionCommand(PlaybackCommands.SetSkipSilence, Bundle.EMPTY),
        Bundle().apply { putBoolean(PlaybackCommands.SkipSilenceKey, enabled) },
    )
}

/** What the service has it set to. Asked once: the screen owns the answer from then on. */
suspend fun MediaController.skipSilence(): Boolean =
    sendCustomCommand(SessionCommand(PlaybackCommands.GetSkipSilence, Bundle.EMPTY), Bundle.EMPTY)
        .await()
        .extras
        .getBoolean(PlaybackCommands.SkipSilenceKey)

/**
 * Twenty decibels, as the old player had it: loud enough to rescue a quiet mix, short of the
 * distortion that comes of asking a phone speaker for more.
 *
 * Shared, because the screen turns a fraction of a gesture into millibels and the service clamps
 * what it is handed -- and a ceiling the two disagreed on would be a gesture that stops moving
 * before it reaches the end of its own track.
 */
const val MaxVolumeBoostMillibels = 2_000

/** What the device gives on its own, with nothing added. Beside the ceiling it shares a scale with,
 *  because the screen that turns a gesture into millibels needs both ends of it. */
const val NoVolumeBoost = 0

/**
 * One key per row, numbered by how many times its item has appeared.
 *
 * A lazy list needs them unique -- and the same track or film legitimately sits in a queue more
 * than once -- while a draggable list needs them to survive the move, which an index cannot: the
 * index is the thing that changes.
 */
fun List<MediaItem>.queueKeys(): List<String> {
    val seen = HashMap<String, Int>()
    return map { item ->
        val occurrence = seen.getOrDefault(item.mediaId, 0) + 1
        seen[item.mediaId] = occurrence
        "${item.mediaId}#$occurrence"
    }
}
