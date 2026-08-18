package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun rememberABRepeatState(player: Player): ABRepeatState {
    val scope = rememberCoroutineScope()
    return remember(player) { ABRepeatState(player, scope) }
}

/**
 * Two marks in the film, and the stretch between them played over and over.
 *
 * For a line of dialogue you cannot make out, a dance step, a guitar part. Both marks are taken
 * from wherever the film is when they are set, so setting them is watching until the moment and
 * pressing.
 */
@Stable
class ABRepeatState(private val player: Player, private val scope: CoroutineScope) {

    var pointA: Long? by mutableStateOf(null)
        private set

    var pointB: Long? by mutableStateOf(null)
        private set

    val isLooping: Boolean get() = pointA != null && pointB != null

    /** Sets the start, or clears the pair — there is no B without an A to come back to. */
    fun toggleA() {
        if (pointA != null) {
            reset()
            return
        }
        pointA = player.currentPosition
        restart()
    }

    fun toggleB() {
        if (pointB != null) {
            pointB = null
            stop()
            return
        }
        val a = pointA ?: return
        // A loop that ends before it begins is not a loop.
        val here = player.currentPosition
        if (here <= a) return
        pointB = here
        restart()
    }

    fun reset() {
        stop()
        pointA = null
        pointB = null
    }

    private fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    private var loopJob: Job? = null

    private fun restart() {
        stop()
        val a = pointA ?: return
        val b = pointB ?: return

        // Waits out what is left of the stretch rather than checking ten times a second whether it
        // is over yet. It lands closer to the mark for waking a fraction as often, and while the
        // film is paused the wait simply repeats instead of spinning.
        loopJob = scope.launch {
            while (isActive) {
                val speed = player.playbackParameters.speed.takeIf { it > 0f } ?: NormalSpeed
                val remaining = ((b - player.currentPosition) / speed).toLong()
                if (remaining <= 0L) player.seekTo(a) else delay(remaining.coerceAtLeast(MinWaitMs))
            }
        }
    }
}

private const val NormalSpeed = 1f

/** A seek does not land the instant it is asked for; without a floor the loop would spin waiting. */
private const val MinWaitMs = 50L
