package dev.vayou.core.player

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.model.AudioEffectType
import dev.vayou.core.model.EqPreset
import dev.vayou.core.model.PlayerPreferences
import kotlin.math.abs

/**
 * Everything attached to the audio session: the equalizer, the two single-knob effects, and the
 * night-mode limiter.
 *
 * Its own class rather than fields on the service, because it is the one part of playback with a
 * lifecycle of its own -- every one of these has to be rebuilt when the session id changes and
 * released when it goes away, and the framework throws if that is got wrong on any of them.
 *
 * Nothing is attached until it is turned up. An `Equalizer` on a session costs a processing stage
 * whether or not its bands are flat, and someone who never opens the sheet should not pay for one.
 *
 * `Virtualizer` is deprecated as of API 36 in favour of `Spatializer`, and the two are not the same
 * control: `Spatializer` is a property of the output route that the system decides, with no
 * strength to set and nothing an app can offer a listener. The knob stays until there is a
 * replacement for it.
 */
@Suppress("DEPRECATION")
@OptIn(UnstableApi::class)
internal class AudioEffects {

    private var sessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var equalizer: Equalizer? = null
    private var limiter: DynamicsProcessing? = null
    private var enhancer: LoudnessEnhancer? = null

    /** Kept across a change of session so the boost survives the effect being rebuilt under it. */
    private var boostMillibels: Int = NoVolumeBoost
    private val strengthEffects = mutableMapOf<AudioEffectType, AudioEffect>()

    /** The session changed under us: rebuild against the new one, honouring [preferences]. */
    fun bind(sessionId: Int, preferences: PlayerPreferences) {
        release()
        this.sessionId = sessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return

        attempt {
            equalizer = Equalizer(EffectPriority, sessionId).apply {
                if (preferences.equalizerBandGains.isEmpty()) {
                    applyPresetGains(preferences.equalizerPreset)
                } else {
                    preferences.equalizerBandGains.forEachIndexed { band, gain ->
                        if (band < numberOfBands) setBandLevel(band.toShort(), gain.toShort())
                    }
                }
                enabled = preferences.equalizerEnabled
            }
        }

        AudioEffectType.entries.forEach { type ->
            val strength = preferences.effectStrength(type)
            if (strength > 0) setStrength(type, strength)
        }

        if (preferences.nightModeEnabled) setNightMode(true)
        if (boostMillibels > NoVolumeBoost) setVolumeBoost(boostMillibels)
    }

    /**
     * Louder than the device is willing to go, for a film mixed too quietly to hear on a phone.
     *
     * A gain on the session rather than a bigger number handed to the stream: the stream is already
     * at its top when this starts, and what is left is amplification. Zero releases the enhancer
     * rather than leaving it enabled at nothing -- an effect on the session costs a processing stage
     * whether or not it is doing anything.
     */
    fun setVolumeBoost(millibels: Int) {
        boostMillibels = millibels.coerceIn(NoVolumeBoost, MaxVolumeBoostMillibels)
        if (boostMillibels == NoVolumeBoost) {
            attempt { enhancer?.release() }
            enhancer = null
            return
        }
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return
        val active = enhancer ?: attempt { LoudnessEnhancer(sessionId) }?.also { enhancer = it } ?: return
        attempt {
            active.setTargetGain(boostMillibels)
            active.enabled = true
        }
    }

    /**
     * Whether this device will amplify at all.
     *
     * Answered by building one and letting it go, which is the only answer the framework gives --
     * and it is asked once, when the screen binds, rather than every time the gesture passes the
     * top of the range.
     */
    fun isVolumeBoostSupported(): Boolean {
        if (enhancer != null) return true
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return false
        val probe = attempt { LoudnessEnhancer(sessionId) } ?: return false
        attempt { probe.release() }
        return true
    }

    /**
     * Night mode: a limiter across both channels, not a change to the volume.
     *
     * What makes a film unwatchable late at night is its range rather than its level -- the dialogue
     * sits thirty decibels under the explosion, so the volume that makes one audible makes the other
     * a problem for the neighbours. A limiter pulls the peaks down and lifts what is left, which is
     * what a television's own "night mode" does.
     *
     * Attached on demand and released when turned off, as everything else here is: a processing
     * stage on the session costs whether or not it is doing anything.
     *
     * `DynamicsProcessing` arrived in API 28. Below that the menu item is hidden rather than left
     * to do nothing -- see [isNightModeSupported].
     */
    fun setNightMode(enabled: Boolean) {
        if (!isNightModeSupported) return
        if (!enabled || sessionId == C.AUDIO_SESSION_ID_UNSET) {
            attempt { limiter?.release() }
            limiter = null
            return
        }
        if (limiter != null) return
        attempt {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                ChannelCount,
                false, 0,
                false, 0,
                false, 0,
                true,
            ).build()
            limiter = DynamicsProcessing(EffectPriority, sessionId, config).apply {
                setLimiterAllChannelsTo(
                    DynamicsProcessing.Limiter(
                        true,
                        true,
                        0,
                        LimiterAttackMs,
                        LimiterReleaseMs,
                        LimiterRatio,
                        LimiterThresholdDb,
                        LimiterPostGainDb,
                    ),
                )
                this.enabled = true
            }
        }
    }

    /** The bands as they stand, or null while nothing is attached. */
    fun bands(): EqualizerBands? {
        val eq = equalizer ?: return null
        return attempt {
            val count = eq.numberOfBands.toInt()
            EqualizerBands(
                // The framework reports centre frequencies in millihertz.
                centreFreqsHz = IntArray(count) { eq.getCenterFreq(it.toShort()) / 1000 },
                levelsMillibels = IntArray(count) { eq.getBandLevel(it.toShort()).toInt() },
                minMillibels = eq.bandLevelRange[0].toInt(),
                maxMillibels = eq.bandLevelRange[1].toInt(),
            )
        }
    }

    fun setEnabled(enabled: Boolean) = attempt { equalizer?.enabled = enabled }

    fun setBandLevel(band: Short, millibels: Short) = attempt { equalizer?.setBandLevel(band, millibels) }

    fun applyPreset(preset: EqPreset) = attempt { equalizer?.applyPresetGains(preset) }

    /**
     * The effects the device implements. Hardware that reports no strength control would leave a
     * knob that moves and is not heard, so the caller is told to hide it.
     */
    fun supportedStrengthEffects(): Set<AudioEffectType> = AudioEffectType.entries
        .filterTo(mutableSetOf()) { type ->
            when (val effect = strengthEffect(type)) {
                is BassBoost -> attempt { effect.strengthSupported } == true
                is Virtualizer -> attempt { effect.strengthSupported } == true
                else -> false
            }
        }

    /** Zero disables the effect outright, so a knob at the bottom costs no processing. */
    fun setStrength(type: AudioEffectType, strength: Int) {
        val effect = strengthEffect(type) ?: return
        attempt {
            when (effect) {
                is BassBoost -> effect.setStrength(strength.toShort())
                is Virtualizer -> effect.setStrength(strength.toShort())
            }
            effect.enabled = strength > 0
        }
    }

    fun release() {
        attempt { equalizer?.release() }
        equalizer = null
        attempt { limiter?.release() }
        limiter = null
        // The enhancer goes; the gain it was set to does not. The session is about to be replaced
        // and the listener has not asked to be quieter.
        attempt { enhancer?.release() }
        enhancer = null
        strengthEffects.values.forEach { effect -> attempt { effect.release() } }
        strengthEffects.clear()
        sessionId = C.AUDIO_SESSION_ID_UNSET
    }

    /**
     * The effect for [type], attached on first use.
     *
     * Bass boost and virtualizer share no supertype in the framework beyond `AudioEffect`, which
     * carries neither of the two calls that matter. That is the only reason the branches exist.
     */
    private fun strengthEffect(type: AudioEffectType): AudioEffect? {
        strengthEffects[type]?.let { return it }
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return null
        return attempt {
            when (type) {
                AudioEffectType.BASS_BOOST -> BassBoost(EffectPriority, sessionId)
                AudioEffectType.VIRTUALIZER -> Virtualizer(EffectPriority, sessionId)
            }
        }?.also { strengthEffects[type] = it }
    }
}

/** What the equalizer looks like right now, in the shape the screen draws. */
@OptIn(UnstableApi::class)
internal class EqualizerBands(
    val centreFreqsHz: IntArray,
    val levelsMillibels: IntArray,
    val minMillibels: Int,
    val maxMillibels: Int,
)

@OptIn(UnstableApi::class)
private fun Equalizer.applyPresetGains(preset: EqPreset) {
    val gains = preset.gains
    val range = bandLevelRange
    for (band in 0 until numberOfBands) {
        val centreHz = getCenterFreq(band.toShort()) / 1000
        // The curves are written at five frequencies; a device may split the spectrum anywhere, so
        // each band takes the gain of whichever of the five it sits closest to.
        val gain = gains.minByOrNull { abs(it.key - centreHz) }?.value ?: 0
        setBandLevel(band.toShort(), gain.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
    }
}

/**
 * Any of these can fail: the effect may be missing, in use by another app, or refused outright, and
 * the framework says so by throwing. None of it is worth losing playback over.
 */
private inline fun <T> attempt(block: () -> T): T? = try {
    block()
} catch (e: RuntimeException) {
    Log.w(Tag, "audio effect unavailable", e)
    null
}

/** Zero, the value the framework documents for an app with no claim over another's effects. */
/** Stereo. A limiter set per channel on a session that turns out to be mono is still stereo here. */
private const val ChannelCount = 2

private const val LimiterAttackMs = 50f

private const val LimiterReleaseMs = 400f

private const val LimiterRatio = 6f

private const val LimiterThresholdDb = -12f

/** Back up what the limiter took off the peaks, so quiet passages gain rather than everything losing. */
private const val LimiterPostGainDb = 3f

private const val EffectPriority = 0

private const val Tag = "AudioEffects"

/** Whether this device can run the night-mode limiter at all. `DynamicsProcessing` is API 28. */
@OptIn(UnstableApi::class)
val isNightModeSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
