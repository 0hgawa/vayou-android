package dev.vayou.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.vayou.core.model.EqPreset
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.player.service.applyEqualizerPreset
import dev.vayou.core.player.service.getEqualizerBands
import dev.vayou.core.player.service.setEqualizerBandLevel
import dev.vayou.core.player.service.setEqualizerEnabled
import kotlin.math.abs
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class EqBand(
    val index: Short,
    val centerFreqHz: Int,
    val levelMillibels: Int,
    val minMillibels: Int,
    val maxMillibels: Int,
)

@Stable
class EqualizerState(
    private val controller: MediaController,
    initialEnabled: Boolean,
    initialPreset: EqPreset,
    private val onSave: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
) {
    var isEnabled by mutableStateOf(initialEnabled)
        private set

    var preset by mutableStateOf(initialPreset)
        private set

    var bands by mutableStateOf<List<EqBand>>(emptyList())
        private set

    val isAvailable: Boolean get() = bands.isNotEmpty()

    suspend fun initialize(player: Player) {
        coroutineScope {
            refreshBands()
            val listener = object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    launch { refreshBands() }
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

    private suspend fun refreshBands() {
        val result = controller.getEqualizerBands() ?: return
        val (bandTriples, range) = result
        bands = bandTriples.map { (index, centerHz, level) ->
            EqBand(
                index = index,
                centerFreqHz = centerHz,
                levelMillibels = level.toInt(),
                minMillibels = range.first.toInt(),
                maxMillibels = range.second.toInt(),
            )
        }
    }

    fun updateEnabled(enabled: Boolean) {
        isEnabled = enabled
        controller.setEqualizerEnabled(enabled)
        onSave { it.copy(equalizerEnabled = enabled) }
    }

    fun setBandLevel(index: Short, level: Int) {
        val first = bands.firstOrNull() ?: return
        val clamped = level.coerceIn(first.minMillibels, first.maxMillibels)
        controller.setEqualizerBandLevel(index, clamped.toShort())
        bands = bands.map { if (it.index == index) it.copy(levelMillibels = clamped) else it }
        preset = EqPreset.CUSTOM
        onSave {
            it.copy(
                equalizerPreset = EqPreset.CUSTOM,
                equalizerBandGains = bands.map { b -> b.levelMillibels },
            )
        }
    }

    fun applyPreset(newPreset: EqPreset) {
        if (newPreset == EqPreset.CUSTOM) return
        controller.applyEqualizerPreset(newPreset)
        preset = newPreset
        val gains = newPreset.gains
        val first = bands.firstOrNull()
        val min = first?.minMillibels ?: Int.MIN_VALUE
        val max = first?.maxMillibels ?: Int.MAX_VALUE
        bands = bands.map { band ->
            val gain = gains.minByOrNull { abs(it.key - band.centerFreqHz) }?.value ?: 0
            band.copy(levelMillibels = gain.coerceIn(min, max))
        }
        onSave { it.copy(equalizerPreset = newPreset, equalizerBandGains = emptyList()) }
    }
}

@Composable
fun rememberEqualizerState(
    player: Player?,
    controller: MediaController?,
    preferences: PlayerPreferences,
    onSave: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
): EqualizerState? {
    val ctrl = controller ?: return null
    val p = player ?: return null
    val state = remember(ctrl) {
        EqualizerState(
            controller = ctrl,
            initialEnabled = preferences.equalizerEnabled,
            initialPreset = preferences.equalizerPreset,
            onSave = onSave,
        )
    }
    LaunchedEffect(p, ctrl) { state.initialize(p) }
    return state
}
