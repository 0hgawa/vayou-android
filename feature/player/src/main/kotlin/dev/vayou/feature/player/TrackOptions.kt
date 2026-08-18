package dev.vayou.feature.player

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.player.ui.TracksState

/**
 * The tracks of one kind as rows for [PlayerOptionsSheet].
 *
 * [offLabel] only where none is a real answer: a file with no subtitle selected is the normal case,
 * one playing with its audio disabled is a fault.
 */
@OptIn(UnstableApi::class)
@Composable
fun trackOptions(state: TracksState, offLabel: String?): List<PlayerOption> = buildList {
    if (offLabel != null) {
        add(PlayerOption(label = offLabel, isSelected = state.isOff, onSelect = state::turnOff))
    }
    state.tracks.forEach { track ->
        add(
            PlayerOption(
                label = track.label,
                isSelected = track.isSelected,
                onSelect = { state.select(track) },
            ),
        )
    }
}
