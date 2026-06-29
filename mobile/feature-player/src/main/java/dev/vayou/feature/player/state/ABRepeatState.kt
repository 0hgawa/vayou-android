package dev.vayou.feature.player.state

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
    return remember { ABRepeatState(player, scope) }
}

@Stable
class ABRepeatState(
    private val player: Player,
    private val scope: CoroutineScope,
) {
    var pointA: Long? by mutableStateOf(null)
        private set
    var pointB: Long? by mutableStateOf(null)
        private set

    private var loopJob: Job? = null

    fun toggleA() {
        if (pointA != null) {
            reset()
        } else {
            pointA = player.currentPosition
            restartLoop()
        }
    }

    fun toggleB() {
        if (pointB != null) {
            loopJob?.cancel()
            loopJob = null
            pointB = null
        } else {
            val a = pointA ?: return
            val current = player.currentPosition
            if (current <= a) return
            pointB = current
            restartLoop()
        }
    }

    fun reset() {
        loopJob?.cancel()
        loopJob = null
        pointA = null
        pointB = null
    }

    private fun restartLoop() {
        loopJob?.cancel()
        val a = pointA ?: return
        val b = pointB ?: return
        loopJob = scope.launch {
            while (isActive) {
                delay(100)
                if (player.currentPosition >= b) {
                    player.seekTo(a)
                }
            }
        }
    }
}
