package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.vayou.core.model.AudioEffectType
import dev.vayou.core.model.EqPreset
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.player.EqualizerBand
import dev.vayou.core.player.applyEqualizerPreset
import dev.vayou.core.player.equalizerBands
import dev.vayou.core.player.setAudioEffectStrength
import dev.vayou.core.player.setEqualizerBandLevel
import dev.vayou.core.player.setEqualizerEnabled
import dev.vayou.core.player.supportedAudioEffects
import kotlin.math.abs
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The equalizer as the screen sees it.
 *
 * Two copies of the truth, and deliberately: the service owns the effect, and this holds what was
 * last sent to it. A band dragged across a phone is a hundred commands a second, and reading each
 * one back before drawing would put the session round-trip inside the drag.
 */
@Stable
class EqualizerState(
    private val controller: MediaController,
    initialEnabled: Boolean,
    initialPreset: EqPreset,
    initialStrengths: Map<AudioEffectType, Int>,
    private val onSave: (PlayerPreferences.() -> PlayerPreferences) -> Unit,
) {
    var isEnabled by mutableStateOf(initialEnabled)
        private set

    var preset by mutableStateOf(initialPreset)
        private set

    var bands by mutableStateOf<List<EqualizerBand>>(emptyList())
        private set

    var strengths by mutableStateOf(initialStrengths)
        private set

    /** Null until [loadEffectSupport] runs; what the device cannot do is never offered. */
    var supportedEffects by mutableStateOf<Set<AudioEffectType>?>(null)
        private set

    /** False on a device with no equalizer, and while the session is being rebuilt. */
    val isAvailable: Boolean get() = bands.isNotEmpty()

    /** True when there is nothing to undo: a flat curve and no effect turned up. */
    val isDefault: Boolean
        get() = preset == EqPreset.FLAT && strengths.values.all { it == 0 }

    /**
     * Follows the player for as long as the screen is up.
     *
     * The audio session is rebuilt without anyone asking, and a new one means new bands -- possibly
     * a different number of them, since how a device splits the spectrum is the device's business.
     */
    suspend fun follow(player: Player) {
        coroutineScope {
            refresh()
            val listener = object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    launch { refresh() }
                }
            }
            try {
                player.addListener(listener)
                awaitCancellation()
            } finally {
                player.removeListener(listener)
            }
        }
    }

    private suspend fun refresh() {
        bands = controller.equalizerBands().orEmpty()
    }

    suspend fun loadEffectSupport() {
        if (supportedEffects == null) supportedEffects = controller.supportedAudioEffects()
    }

    fun updateEnabled(enabled: Boolean) {
        isEnabled = enabled
        controller.setEqualizerEnabled(enabled)
        onSave { copy(equalizerEnabled = enabled) }
    }

    fun setBandLevel(band: Short, millibels: Int) {
        val first = bands.firstOrNull() ?: return
        val clamped = millibels.coerceIn(first.minMillibels, first.maxMillibels)
        controller.setEqualizerBandLevel(band, clamped)
        bands = bands.map { if (it.index == band) it.copy(levelMillibels = clamped) else it }
        preset = EqPreset.CUSTOM
        // The gains and not the preset, since there is no longer a preset that describes them.
        onSave {
            copy(
                equalizerPreset = EqPreset.CUSTOM,
                equalizerBandGains = bands.map { it.levelMillibels },
            )
        }
    }

    fun applyPreset(newPreset: EqPreset) {
        if (newPreset == EqPreset.CUSTOM) return
        controller.applyEqualizerPreset(newPreset)
        preset = newPreset
        val first = bands.firstOrNull()
        val min = first?.minMillibels ?: Int.MIN_VALUE
        val max = first?.maxMillibels ?: Int.MAX_VALUE
        bands = bands.map { band ->
            // The same nearest-frequency rule the service applies, so the drawn curve is the one
            // the effect is actually running.
            val gain = newPreset.gains.minByOrNull { abs(it.key - band.centreFreqHz) }?.value ?: 0
            band.copy(levelMillibels = gain.coerceIn(min, max))
        }
        // No gains stored: the preset is the shorter description, and it survives a device whose
        // bands are cut differently.
        onSave { copy(equalizerPreset = newPreset, equalizerBandGains = emptyList()) }
    }

    fun setStrength(type: AudioEffectType, strength: Int) {
        val clamped = strength.coerceIn(0, AudioEffectType.MAX_STRENGTH)
        controller.setAudioEffectStrength(type, clamped)
        strengths = strengths + (type to clamped)
        onSave { withEffectStrength(type, clamped) }
    }

    /**
     * Back to a flat curve with the effects off, in one action.
     *
     * Flat *is* the reset for the bands, and it is already a tile on the sheet. What had no way
     * back were the effects: the only way down from a bass boost was to find the same
     * thousand-step track and drag it to the left edge by hand.
     */
    fun reset() {
        applyPreset(EqPreset.FLAT)
        supportedEffects.orEmpty().forEach { setStrength(it, 0) }
    }
}

@Composable
fun rememberEqualizerState(
    player: Player,
    preferences: PlayerPreferences,
    onSave: (PlayerPreferences.() -> PlayerPreferences) -> Unit,
): EqualizerState? {
    val controller = player as? MediaController ?: return null
    val state = remember(controller) {
        EqualizerState(
            controller = controller,
            initialEnabled = preferences.equalizerEnabled,
            initialPreset = preferences.equalizerPreset,
            initialStrengths = AudioEffectType.entries.associateWith(preferences::effectStrength),
            onSave = onSave,
        )
    }
    LaunchedEffect(state) { state.follow(player) }
    return state
}
