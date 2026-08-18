package dev.vayou.feature.player

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.vayou.core.player.PlaybackCommands
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.components.VayouBottomSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults

/**
 * How long to keep playing before stopping on its own.
 *
 * The lengths are the ones people actually pick: enough to finish an episode, or to fall asleep to
 * one. "When this ends" is here because for a film that is the only honest answer — the length of
 * what is playing is not a number anyone knows in minutes.
 */
@Composable
fun SleepTimerSheet(state: SleepTimerState, onDismiss: () -> Unit) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = stringResource(R.string.sleep_timer))

        if (state.isArmed) {
            CheckedRow(
                text = stringResource(R.string.sleep_timer_off),
                isSelected = false,
                onClick = {
                    state.set(PlaybackCommands.Off)
                    onDismiss()
                },
            )
        }

        CheckedRow(
            text = stringResource(R.string.sleep_timer_end_of_track),
            isSelected = state.minutes == PlaybackCommands.EndOfTrack,
            onClick = {
                state.set(PlaybackCommands.EndOfTrack)
                onDismiss()
            },
        )

        Lengths.forEach { minutes ->
            CheckedRow(
                text = pluralStringResource(R.plurals.sleep_timer_minutes, minutes, minutes),
                isSelected = state.minutes == minutes,
                // The remaining time only on the one that is running, since it is the answer to
                // "how much is left" and every other row is an answer to "how much from now".
                trailing = formatTime(state.remainingMs).takeIf { state.minutes == minutes && state.isCounting },
                onClick = {
                    state.set(minutes)
                    onDismiss()
                },
            )
        }

        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}

/** Long enough to finish an episode, or to fall asleep to one. */
private val Lengths = listOf(15, 30, 45, 60, 90)
