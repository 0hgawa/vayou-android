package dev.vayou.feature.player.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.EqPreset
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.components.VayouFilterChip
import dev.vayou.core.ui.designsystem.components.VayouSwitch
import dev.vayou.core.ui.designsystem.components.VayouVerticalSlider
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.feature.player.extensions.nameRes
import dev.vayou.feature.player.state.EqualizerState

@Composable
fun BoxScope.EqualizerView(
    show: Boolean,
    equalizerState: EqualizerState?,
) {
    OverlayView(
        show = show,
        title = stringResource(R.string.equalizer),
        maxHeightFraction = 0.45f,
        trailingAction = equalizerState?.let {
            {
                VayouSwitch(
                    checked = it.isEnabled,
                    onCheckedChange = it::updateEnabled,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = OverlayContentPadding)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqPreset.entries
                .filter { it != EqPreset.CUSTOM }
                .forEach { preset ->
                    VayouFilterChip(
                        selected = equalizerState?.preset == preset,
                        enabled = equalizerState?.isAvailable == true,
                        onClick = { equalizerState?.applyPreset(preset) },
                        label = stringResource(preset.nameRes()),
                    )
                }
        }

        if (equalizerState?.isAvailable == true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = OverlayContentPadding)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                equalizerState.bands.forEach { band ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val db = band.levelMillibels / 100
                        Text(
                            text = if (db >= 0) "+$db" else "$db",
                            style = VayouTheme.typography.labelSmall,
                            color = VayouTheme.colors.onSurface,
                        )
                        VayouVerticalSlider(
                            value = band.levelMillibels.toFloat(),
                            onValueChange = { equalizerState.setBandLevel(band.index, it.toInt()) },
                            modifier = Modifier.weight(1f),
                            valueRange = band.minMillibels.toFloat()..band.maxMillibels.toFloat(),
                            showCenterHighlight = true,
                        )
                        Text(
                            text = band.centerFreqHz.toFreqLabel(),
                            style = VayouTheme.typography.labelSmall,
                            color = VayouTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun Int.toFreqLabel(): String = if (this >= 1000) "${this / 1000}k" else "$this"
