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
import kotlinx.coroutines.launch

@Composable
fun rememberSleepTimerState(player: Player): SleepTimerState {
    val scope = rememberCoroutineScope()
    return remember { SleepTimerState(player, scope) }
}

@Stable
class SleepTimerState(
    private val player: Player,
    private val scope: CoroutineScope,
) {
    var remainingMs: Long? by mutableStateOf(null)
        private set

    var activeMinutes: Int? by mutableStateOf(null)
        private set

    val formattedRemaining: String?
        get() = remainingMs?.let { ms ->
            "%02d:%02d".format(ms / 60_000, (ms % 60_000) / 1_000)
        }

    private var job: Job? = null

    fun setTimer(minutes: Int) {
        job?.cancel()
        activeMinutes = minutes
        remainingMs = minutes * 60_000L
        job = scope.launch {
            while (true) {
                delay(1_000)
                val current = remainingMs ?: break
                if (current <= 1_000) {
                    remainingMs = null
                    player.pause()
                    break
                }
                remainingMs = current - 1_000
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        remainingMs = null
        activeMinutes = null
    }
}
