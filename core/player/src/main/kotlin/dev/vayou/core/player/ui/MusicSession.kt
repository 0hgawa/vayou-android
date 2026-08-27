package dev.vayou.core.player.ui

import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.vayou.core.media.Song
import dev.vayou.core.player.PlaybackService
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/** A track from the library as a queue entry, cover included. */
fun musicMediaItem(song: Song): MediaItem = musicMediaItem(song.uriString, song.title, song.artist, song.artworkUri)

/**
 * A track as a queue entry: address, name, and the picture that goes with it.
 *
 * The media id is the address, which is how the service and the mini controller recognise what is
 * playing.
 *
 * The cover rides along rather than being looked up when the track changes, which is the difference
 * between one source of truth and a race. Looked up, the answer arrives a moment after the track
 * does, and for that moment every screen is describing the track before this one -- which is
 * exactly how the player came to open on the last song's cover. Carried, the picture and the track
 * are the same object: the screen, the notification and the lock screen all read it, and none of
 * them can disagree.
 *
 * Null for a track from a share, which has no album and no picture to name; the app's own mark is
 * what shows then.
 */
fun musicMediaItem(uri: String, title: String, artist: String = "", artworkUri: Uri? = null): MediaItem =
    MediaItem.Builder()
        .setUri(uri)
        .setMediaId(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setTitle(title.takeIf { it.isNotBlank() })
                .setArtist(artist.takeIf { it.isNotBlank() })
                .setArtworkUri(artworkUri)
                .build(),
        )
        .build()

/**
 * Connects to the shared playback service while the host screen is started, and lets go on stop.
 *
 * [onEvents] runs once on connection and again on every player event, so each caller mirrors only
 * the state it needs without re-implementing the connection.
 */
@Composable
fun rememberMusicController(onEvents: (Player) -> Unit = {}): MediaController? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var controller by remember { mutableStateOf<MediaController?>(null) }

    LifecycleStartEffect(Unit) {
        val token = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(context.applicationContext, token).buildAsync()
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = onEvents(player)
        }
        scope.launch {
            val connected = future.await()
            controller = connected
            connected.addListener(listener)
            onEvents(connected)
        }
        onStopOrDispose {
            controller?.removeListener(listener)
            MediaController.releaseFuture(future)
            controller = null
        }
    }

    return controller
}

/**
 * Queues [song] right after whatever is playing.
 *
 * An empty queue has nothing to follow, so the track starts instead -- otherwise the action would
 * appear to do nothing.
 */
fun MediaController.playNext(songs: List<Song>) {
    val items = songs.map(::musicMediaItem)
    if (items.isEmpty()) return
    if (mediaItemCount == 0) {
        setMediaItems(items)
        prepare()
        play()
    } else {
        addMediaItems(currentMediaItemIndex + 1, items)
    }
}

/**
 * On the end of the queue rather than after what is playing.
 *
 * The other half of [playNext]: one is "after this", the other is "when everything else is done".
 * A listener who wants both has no way to say so with only one of them.
 */
fun MediaController.addToQueue(songs: List<Song>) {
    val items = songs.map(::musicMediaItem)
    if (items.isEmpty()) return
    if (mediaItemCount == 0) {
        setMediaItems(items)
        prepare()
        play()
    } else {
        addMediaItems(items)
    }
}
