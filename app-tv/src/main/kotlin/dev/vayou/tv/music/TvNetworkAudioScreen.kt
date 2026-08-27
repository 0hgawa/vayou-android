package dev.vayou.tv.music

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import dev.vayou.core.player.ui.musicMediaItem
import dev.vayou.core.player.ui.rememberMusicController
import dev.vayou.tv.R
import dev.vayou.tv.TvMessage

/**
 * A track from a share, on the music screen rather than the film one.
 *
 * A route of its own, and not a mode of the video player. The two have almost nothing in common
 * once the sound is the subject: no legend, no aspect, no speed, and a cover where the picture
 * would be. This is where the phone puts music too, so a share opened from either behaves the same.
 */
@Composable
fun TvNetworkAudioScreen(onBack: () -> Unit, viewModel: TvNetworkAudioViewModel = hiltViewModel()) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val controller = rememberMusicController()

    // Set once the folder and the service have both answered, and once only: this screen is
    // recomposed on every tick of the clock under the cover, and handing the queue over again would
    // restart the track each time.
    var isQueued by remember { mutableStateOf(false) }
    LaunchedEffect(queue, controller) {
        val ready = queue ?: return@LaunchedEffect
        val player = controller ?: return@LaunchedEffect
        if (isQueued) return@LaunchedEffect
        isQueued = true
        player.setMediaItems(
            // Named by the end of its own address, decoded, or a space would read as `%20`. The
            // file's own tags replace this the moment the player has read them.
            ready.tracks.map { uri -> musicMediaItem(uri, Uri.decode(uri.substringAfterLast('/'))) },
            ready.startIndex,
            0,
        )
        player.prepare()
        player.play()
    }

    // Leaving the screen leaves the music, as leaving the film leaves the film: there is no
    // notification on a television to bring it back from.
    //
    // Hung off the screen going away rather than off the back key, and that is the difference
    // between working and not: the queue beside the sleeve answers back too, and a stop wired to
    // the key would have silenced the track for the crime of closing a list.
    DisposableEffect(controller) {
        val active = controller
        onDispose { active?.stop() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (controller == null || !isQueued) {
            TvMessage(stringResource(R.string.opening))
        } else {
            TvNowPlaying(controller)
        }
    }
}
