package dev.vayou.feature.player.extensions

import dev.vayou.core.model.EqPreset
import dev.vayou.core.ui.R

fun EqPreset.nameRes(): Int = when (this) {
    EqPreset.FLAT -> R.string.eq_preset_flat
    EqPreset.CLASSICAL -> R.string.eq_preset_classical
    EqPreset.DANCE -> R.string.eq_preset_dance
    EqPreset.FOLK -> R.string.eq_preset_folk
    EqPreset.HEAVY_METAL -> R.string.eq_preset_heavy_metal
    EqPreset.HIP_HOP -> R.string.eq_preset_hip_hop
    EqPreset.JAZZ -> R.string.eq_preset_jazz
    EqPreset.POP -> R.string.eq_preset_pop
    EqPreset.ROCK -> R.string.eq_preset_rock
    EqPreset.CUSTOM -> R.string.eq_preset_custom
}
