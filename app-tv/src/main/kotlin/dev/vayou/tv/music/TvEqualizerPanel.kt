package dev.vayou.tv.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.media3.session.MediaController
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.model.EqPreset
import dev.vayou.core.player.R as PlayerR
import dev.vayou.core.player.applyEqualizerPreset
import dev.vayou.core.player.setEqualizerEnabled
import dev.vayou.tv.R
import dev.vayou.tv.TvChoiceRow
import dev.vayou.tv.TvRowGap
import dev.vayou.tv.TvTitleInset
import dev.vayou.tv.claim

/**
 * The curves the sound can be put on, as a list to walk with a remote.
 *
 * Curves and nothing else. The phone offers five sliders under its presets and a television should
 * not: a slider is a thing you drag, and dragged with four arrow keys it is a dozen presses to move
 * one band of five, times five bands, with the music playing through every step of it. What a
 * listener across a room actually wants is "more voice" or "more bass", and that is what a named
 * curve is. Anyone who wants the sliders has them on the phone, against the same setting.
 *
 * "Off" leads, rather than sitting at the end or hiding behind a switch: it is the row most often
 * wanted after a curve has been tried and disliked, and on a remote the first row is the cheapest
 * one to reach.
 *
 * The sound changes on the press, through the session -- the effects belong to the playback
 * service, and this screen only has a controller to ask with. Remembering the choice is the model's
 * half, so it survives the panel closing.
 */
@Composable
internal fun TvEqualizerPanel(
    controller: MediaController,
    current: EqPreset,
    isOn: Boolean,
    onChosen: (EqPreset, Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // CUSTOM is what the phone's sliders leave behind when a band is moved by hand. It is a state
    // to be in, not a choice to make: offered here it would be a row that says "whatever you last
    // set on the other device", which is not something anyone can decide to want.
    val curves = remember { EqPreset.entries.filter { it != EqPreset.CUSTOM } }

    val focus = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.claim { hasFocus } }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Both halves of the press, as the queue takes them, and for the reason given there:
            // a key-up delivered after the panel has gone reaches the seek bar and scrubs.
            .onPreviewKeyEvent { event ->
                if (event.key != Key.DirectionLeft && event.key != Key.Back) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) onDismiss()
                true
            },
        verticalArrangement = Arrangement.spacedBy(TvTitleInset),
    ) {
        Text(
            text = stringResource(R.string.equalizer),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(TvRowGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                TvChoiceRow(
                    label = stringResource(R.string.equalizer_off),
                    isSelected = !isOn,
                    modifier = Modifier
                        .focusRequester(focus)
                        .onFocusChanged { hasFocus = it.isFocused },
                ) {
                    controller.setEqualizerEnabled(false)
                    onChosen(current, false)
                }
            }
            items(curves) { preset ->
                TvChoiceRow(
                    label = stringResource(preset.tvLabel),
                    isSelected = isOn && preset == current,
                ) {
                    // Turned on first: a curve applied to an equalizer that is off is a press that
                    // does nothing, and the listener has no way of telling that from a curve that
                    // sounds the same as the last one.
                    controller.setEqualizerEnabled(true)
                    controller.applyEqualizerPreset(preset)
                    onChosen(preset, true)
                }
            }
        }
    }
}

/** Named in core, beside the enum, because the phone shows the same list off the same values. */
private val EqPreset.tvLabel: Int
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
