package dev.vayou.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.AudioEffectType
import dev.vayou.core.model.EqPreset
import dev.vayou.core.player.R as PlayerR
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouSelectableTile
import dev.vayou.core.ui.designsystem.components.VayouSelectableTileSpacing
import dev.vayou.core.ui.designsystem.components.VayouSheet
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouSlider
import dev.vayou.core.ui.designsystem.components.VayouSwitch
import dev.vayou.core.ui.designsystem.components.VayouVerticalSlider
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The equalizer.
 *
 * Three blocks, and the spacing says so: the bands, the presets, the whole-curve effects. Inside a
 * block things sit close; between them there is a clear step, which is what stops a screenful of
 * sliders reading as one undifferentiated field of them.
 *
 * The switch and the reset sit in the title row, because both are about the equalizer as a whole
 * rather than about one band of it.
 */
@Composable
fun EqualizerSheet(state: EqualizerState, onDismiss: () -> Unit) {
    VayouSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = TrailingInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VayouSheetTitle(text = stringResource(R.string.equalizer), modifier = Modifier.weight(1f))
            // Only when there is something to undo: on an untouched equalizer this is a control
            // that does nothing, in the corner the eye checks first.
            if (!state.isDefault) {
                VayouIconButton(onClick = state::reset) {
                    Icon(
                        imageVector = VayouIcons.Refresh,
                        contentDescription = stringResource(R.string.reset),
                        tint = VayouTheme.colors.onSurfaceVariant,
                    )
                }
            }
            VayouSwitch(checked = state.isEnabled, onCheckedChange = state::updateEnabled)
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (state.isAvailable) {
                Bands(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Before the height, so [BandsHeight] is the block itself and the step
                        // between blocks is added around it. After, the padding would have come out
                        // of the sliders and left them shorter than asked for.
                        .padding(bottom = SectionSpacing)
                        .padding(horizontal = VayouSheetDefaults.HorizontalPadding)
                        .height(BandsHeight),
                )
            }

            PresetGrid(
                state = state,
                modifier = Modifier.padding(horizontal = VayouSheetDefaults.HorizontalPadding),
            )

            if (state.isAvailable) {
                EffectSliders(
                    state = state,
                    modifier = Modifier
                        .padding(top = SectionSpacingBeforeSlider)
                        .padding(horizontal = VayouSheetDefaults.HorizontalPadding),
                )
            }

            Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
        }
    }
}

/**
 * The bands: what each is set to, the sliders, and the frequency each covers.
 *
 * Every gain is shown, not only the one under the finger. Held back until a drag, the line above
 * the sliders is blank almost always -- and it still has to be laid out, or releasing a slider
 * shifts the row. Shown for all of them the same space carries the curve as numbers, and the block
 * closes symmetrically: values, sliders, frequencies.
 *
 * No unit on the values. Ten of them across a phone leaves no room for one, the frequencies below
 * say what axis this is, and a band marked +3 in an equalizer is not read as anything but decibels.
 */
@Composable
private fun Bands(state: EqualizerState, modifier: Modifier = Modifier) {
    var dragged by remember { mutableStateOf<Short?>(null) }
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            state.bands.forEach { band ->
                val db = band.levelMillibels / MillibelsPerDecibel
                Text(
                    text = if (db > 0) "+$db" else "$db",
                    modifier = Modifier.weight(1f),
                    style = VayouTheme.typography.labelSmall,
                    // The one being dragged is the one being read.
                    color = if (band.index == dragged) {
                        VayouTheme.colors.accent
                    } else {
                        VayouTheme.colors.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = VayouTheme.spacing.sm),
        ) {
            state.bands.forEach { band ->
                VayouVerticalSlider(
                    value = band.levelMillibels.toFloat(),
                    onValueChange = {
                        dragged = band.index
                        state.setBandLevel(band.index, it.toInt())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    valueRange = band.minMillibels.toFloat()..band.maxMillibels.toFloat(),
                    onValueChangeFinished = { dragged = null },
                    showCenterHighlight = true,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            state.bands.forEach { band ->
                Text(
                    text = band.centreFreqHz.asFrequencyLabel(),
                    modifier = Modifier.weight(1f),
                    style = VayouTheme.typography.labelSmall,
                    color = VayouTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The presets, one tile each, two to a row.
 *
 * A grid and not a scrolling row of pills: a row put six of them behind a swipe and showed four,
 * and a preset you cannot see is one you will not try.
 *
 * Under the sliders, not over them. The sliders are what this sheet is; a preset is a shortcut to a
 * shape of them, and a shortcut belongs after the thing it is a shortcut for.
 */
@Composable
private fun PresetGrid(state: EqualizerState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VayouSelectableTileSpacing),
    ) {
        // CUSTOM is what the bands say when they match no preset, not something to pick.
        EqPreset.entries.filter { it != EqPreset.CUSTOM }.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(VayouSelectableTileSpacing)) {
                pair.forEach { preset ->
                    VayouSelectableTile(
                        label = stringResource(preset.label),
                        selected = state.preset == preset,
                        enabled = state.isAvailable,
                        onClick = { state.applyPreset(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // An odd count leaves the last tile its own width rather than twice everyone else's.
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * The single-knob effects, last on the sheet. They colour the whole curve rather than one part of
 * it, and they are the only controls here that are not the equalizer.
 *
 * Only what the device implements is drawn: a knob that moves without being heard is worse than no
 * knob.
 *
 * The name beside the track rather than over it, in a column only as wide as the longer of the two
 * names, so both tracks start and end at the same place and each row reads the way a setting does:
 * what it is, what it is set to, how much. The percentage is the only readout these have -- a
 * band's is written above it in decibels, and "how much bass boost is on" is not a question a thumb
 * position answers.
 */
@Composable
private fun EffectSliders(state: EqualizerState, modifier: Modifier = Modifier) {
    LaunchedEffect(state) { state.loadEffectSupport() }

    val supported = state.supportedEffects ?: return
    Column(
        modifier = modifier.fillMaxWidth(),
        // Far less than the step between blocks, or the two rows would sit further from each other
        // than from the grid above and the grouping would read backwards. Each row is a 48dp slider
        // target, so 8dp of instruction is some 24dp of air.
        verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
    ) {
        AudioEffectType.entries.filter { it in supported }.forEach { type ->
            val strength = state.strengths[type] ?: 0
            Row(
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(type.label),
                    modifier = Modifier.width(EffectLabelWidth),
                    style = VayouSheetDefaults.ControlLabelStyle,
                    color = VayouTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                VayouSlider(
                    value = strength.toFloat(),
                    onValueChange = { state.setStrength(type, it.toInt()) },
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..AudioEffectType.MAX_STRENGTH.toFloat(),
                )
                Text(
                    text = "${strength * 100 / AudioEffectType.MAX_STRENGTH}%",
                    modifier = Modifier.width(EffectValueWidth),
                    style = VayouTheme.typography.labelSmall,
                    color = VayouTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private val EqPreset.label: Int
    get() = when (this) {
        EqPreset.FLAT -> PlayerR.string.eq_preset_flat
        EqPreset.VOCAL -> PlayerR.string.eq_preset_vocal
        EqPreset.CLASSICAL -> PlayerR.string.eq_preset_classical
        EqPreset.DANCE -> PlayerR.string.eq_preset_dance
        EqPreset.FOLK -> PlayerR.string.eq_preset_folk
        EqPreset.HEAVY_METAL -> PlayerR.string.eq_preset_heavy_metal
        EqPreset.HIP_HOP -> PlayerR.string.eq_preset_hip_hop
        EqPreset.JAZZ -> PlayerR.string.eq_preset_jazz
        EqPreset.POP -> PlayerR.string.eq_preset_pop
        EqPreset.ROCK -> PlayerR.string.eq_preset_rock
        EqPreset.CUSTOM -> PlayerR.string.eq_preset_custom
    }

private val AudioEffectType.label: Int
    get() = when (this) {
        AudioEffectType.BASS_BOOST -> R.string.audio_effect_bass_boost
        AudioEffectType.VIRTUALIZER -> R.string.audio_effect_virtualizer
    }

private fun Int.asFrequencyLabel(): String = when {
    this < 1000 -> "${this}Hz"
    this % 1000 == 0 -> "${this / 1000}kHz"
    else -> "${this / 1000}.${this % 1000 / 100}kHz"
}

/**
 * The step between blocks, stated as what the eye should see rather than as what the layout is told.
 *
 * A block that opens with plain text -- the grid of tiles, whose first row is a border -- gets this
 * whole amount. A block that opens with a slider row does not: a slider is a 48dp target around a
 * 4dp track, so it carries about 16dp of nothing above its own label already.
 */
private val SectionSpacing = 40.dp

/** [SectionSpacing] less the target a slider row carries above itself. Different number, same gap. */
private val SectionSpacingBeforeSlider = 24.dp

/** The band block: its two label rows and the sliders between them. */
private val BandsHeight = 168.dp

/** As wide as the longer of the two names, and no wider: every dp here is a dp the track loses. */
private val EffectLabelWidth = 84.dp

/** Fits "100%", so the track does not shift as the number grows a digit. */
private val EffectValueWidth = 36.dp

/** An icon button carries its own padding, so the title row's trailing margin is that much less
 *  than the sheet's own. */
private val TrailingInset = VayouSheetDefaults.HorizontalPadding - 8.dp

private const val MillibelsPerDecibel = 100
