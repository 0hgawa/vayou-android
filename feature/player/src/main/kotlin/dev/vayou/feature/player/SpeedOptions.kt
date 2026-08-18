package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.media3.ui.compose.state.PlaybackSpeedState
import kotlin.math.abs

/**
 * The speeds worth offering, as rows for [PlayerOptionsSheet].
 *
 * A fixed ladder rather than a slider: nobody wants 1.37x, and a list can be hit without looking
 * away from the film.
 */
@Composable
fun speedOptions(state: PlaybackSpeedState): List<PlayerOption> = Speeds.map { speed ->
    PlayerOption(
        label = speed.label(),
        // Compared with a tolerance, because the player reports a float it has round-tripped and
        // an equality check on that misses the very row the viewer just picked.
        isSelected = abs(state.playbackSpeed - speed) < Tolerance,
        onSelect = { state.updatePlaybackSpeed(speed) },
    )
}

/** `1x` rather than `1.0x`, and `1.25x` where the quarter matters. */
private fun Float.label(): String = if (this == toInt().toFloat()) "${toInt()}x" else "${toString().trimEnd('0')}x"

private val Speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private const val Tolerance = 0.01f
