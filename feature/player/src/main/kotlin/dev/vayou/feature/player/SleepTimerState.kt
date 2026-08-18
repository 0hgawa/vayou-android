package dev.vayou.feature.player

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import dev.vayou.core.player.PlaybackCommands
import dev.vayou.core.player.setSleepTimer
import dev.vayou.core.player.sleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberSleepTimerState(controller: MediaController): SleepTimerState {
    val scope = rememberCoroutineScope()
    val state = remember(controller) { SleepTimerState(controller, scope) }

    // Asked for once on arrival: the timer belongs to the service, and a screen opened an hour
    // after one was armed has to be told what it missed rather than assume nothing was set.
    LaunchedEffect(state) { state.refresh() }

    // A second at a time, and only while something is counting, to redraw one line of text.
    LaunchedEffect(state.isCounting) {
        while (state.isCounting) {
            delay(TickMs)
            state.onTick()
        }
    }
    return state
}

/**
 * What the service's timer is doing, for a screen to draw.
 *
 * Only the reading lives here. The counting is the service's, because arming a timer is followed by
 * putting the phone down — the screen goes off, the player is closed — and a countdown owned by a
 * composition would be cancelled by every one of those.
 */
@Stable
class SleepTimerState(private val controller: MediaController, private val scope: CoroutineScope) {

    /** The choice that armed it, so a reopened panel still marks the option it was set from. */
    var minutes: Int by mutableIntStateOf(PlaybackCommands.Off)
        private set

    /** Its own state and not a value read off the deadline: the deadline does not move, and what is
     *  left of it does, so nothing would tell the screen to draw again. */
    var remainingMs: Long by mutableLongStateOf(0L)
        private set

    val isArmed: Boolean get() = minutes != PlaybackCommands.Off

    /** False while waiting on the end of a track, which has no hour to count towards. */
    val isCounting: Boolean get() = remainingMs > 0L

    private var deadlineMs: Long = 0L

    fun set(minutes: Int) {
        scope.launch {
            controller.setSleepTimer(minutes)
            refresh()
        }
    }

    suspend fun refresh() {
        val armed = controller.sleepTimer()
        minutes = armed.minutes
        deadlineMs = armed.deadlineMs
        onTick()
    }

    fun onTick() {
        val left = deadlineMs - SystemClock.elapsedRealtime()
        if (left > 0L) {
            remainingMs = left
            return
        }
        remainingMs = 0L
        if (deadlineMs > 0L) {
            // Ran out while this was on screen.
            deadlineMs = 0L
            minutes = PlaybackCommands.Off
        }
    }
}

private const val TickMs = 1_000L
