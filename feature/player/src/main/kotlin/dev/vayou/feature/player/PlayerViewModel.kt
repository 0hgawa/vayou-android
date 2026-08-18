package dev.vayou.feature.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.mediarouter.media.MediaRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.common.OpenSubtitlesHasher
import dev.vayou.core.common.di.ApplicationScope
import dev.vayou.core.data.models.OpenSubtitleResult
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.OpenSubtitlesRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.domain.GetSortedVideosUseCase
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Resume
import dev.vayou.core.player.PlaybackService
import dev.vayou.core.player.addSubtitle
import dev.vayou.core.player.externalSubtitle
import dev.vayou.core.player.setNightMode
import dev.vayou.core.player.setScrubbing
import dev.vayou.core.player.setSubtitleDelay
import dev.vayou.core.player.ui.TranslationLookaheadMs
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the player for as long as the screen exists, and remembers where each film was left.
 *
 * In the ViewModel and not the composable: a rotation tears the composition down and would take a
 * decoder, a surface and the playback position with it, and the file would start over.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val getSortedVideos: GetSortedVideosUseCase,
    private val openSubtitles: OpenSubtitlesRepository,
    private val preferencesRepository: PreferencesRepository,
    // Not viewModelScope for the writes: the last one is worth more than the others and happens
    // exactly as this screen is going away, which is when viewModelScope is being cancelled.
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    /**
     * Null until the service answers, which is a frame or two after this screen appears.
     *
     * A controller and not a player: what plays lives in [PlaybackService] now, so that a film
     * paused to answer the door is still there afterwards. Everything below drives it through the
     * same Player interface an ExoPlayer offered, which is why almost none of it had to change.
     */
    var controller: MediaController? by mutableStateOf(null)
        private set

    private var openedUri: String? = null
    private var pending: PlaybackRequest? = null

    private val listener = object : Player.Listener {
        // Covers pausing, reaching the end, and the player being stopped under us. Saving on a
        // timer instead would write over and over for a file nobody is touching.
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) savePosition()
        }

        // The one moment the outgoing file can still be asked where it got to. By the time the
        // transition below is announced, the player is already talking about the next.
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) return
            val leaving = oldPosition.mediaItem ?: return
            write(
                uri = leaving.mediaId,
                position = oldPosition.positionMs,
                duration = leaving.mediaMetadata.durationMs,
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // The first item was given its position when the queue was set. Everything after
            // arrives at zero and has to be told.
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
            // Reached on its own rather than asked for: the film before it ended. Whether to carry
            // on into the next one is the viewer's to say, and a queue that runs a folder to the
            // end unasked is what the setting exists to stop.
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                !preferencesRepository.playerPreferences.value.autoplay
            ) {
                controller?.pause()
            }
            onItemOpened(mediaItem?.mediaId ?: return)
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val connected = future.get()
                connected.addListener(listener)
                controller = connected
                // Whatever was asked for while the service was still answering.
                pending?.let { open(it) }
                pending = null
            },
            // The controller may only be touched from the thread that built it.
            ContextCompat.getMainExecutor(context),
        )
    }

    internal fun open(request: PlaybackRequest) {
        // Held until the service answers rather than dropped: the screen asks once, and the answer
        // arrives a frame or two later.
        val player = controller ?: run {
            pending = request
            return
        }
        // Set before suspending, so a second call while the lookup is in flight sees it. This runs
        // again on every configuration change, and opening afresh would seek back to zero.
        if (openedUri == request.uri) return
        openedUri = request.uri

        viewModelScope.launch {
            val preferences = preferencesRepository.playerPreferences.value
            val queue = queueFor(request)
            val start = queue.indexOfFirst { it.mediaId == request.uri }.coerceAtLeast(0)
            // A caller that names a position means it. Only when none is given does the app go
            // looking for where this file was last left -- and only if it was asked to remember.
            val saved = mediaRepository.getVideoState(request.uri)
            // Never a channel. A broadcast has no position to return to -- what was on at that
            // moment is gone -- and asking a receiver to start a live stream three minutes in is a
            // load it rejects, after which it moves to the next item in the queue.
            val from = request.startPositionMs
                ?: saved?.position?.takeIf { preferences.resume == Resume.YES && !request.isLive }
                ?: StartOfFile

            player.setMediaItems(queue, start, from)
            player.setPlaybackSpeed(preferences.defaultPlaybackSpeed)
            player.prepare()
            // A film opened is a film to watch. Nothing here is a reason to hold it on its first
            // frame -- [PlayerPreferences.autoplay] governs what happens at the *end* of one.
            player.play()

            // The first item of a queue never reaches [onItemOpened] -- its transition is announced
            // as a change of playlist -- so what it remembers is applied from the read above.
            applySubtitleDelay(saved?.subtitleDelayMilliseconds ?: NoSubtitleDelay)

            // After preparing, because a subtitle is added by replacing the item playing, and there
            // is nothing to replace until there is something playing.
            request.subtitles.forEach { player.addSubtitle(context.externalSubtitle(it.uri, it.name, it.isSelected)) }
        }
    }

    /**
     * The file that was asked for, and the rest of its folder behind it.
     *
     * Sorted the way the library sorts, because "next" has to mean the one below it on the screen
     * the viewer just came from. A file opened from another app is in no folder this app knows, so
     * it plays alone — unless that app sent a running order of its own, which wins.
     */
    private suspend fun queueFor(request: PlaybackRequest): List<MediaItem> {
        // A channel on a television goes alone.
        //
        // The running order behind a channel is the hundred either side of it, so that next and
        // previous zap. A receiver treats that list as a playlist and steps to the next entry the
        // moment one fails to load -- and a channel list always has entries this or that receiver
        // cannot decode. One dead channel then walks the viewer through the neighbourhood, which is
        // a far worse answer than the one channel simply not playing.
        if (request.isLive && isCasting()) {
            return listOf(mediaItemFor(request.uri, request.title, null, isLive = true))
        }
        // A caller that sent a running order gets its running order.
        if (request.queue.isNotEmpty()) {
            return request.queue.mapIndexed { index, uri ->
                mediaItemFor(uri, request.queueTitles.getOrNull(index) ?: titleFor(uri), null, request.isLive)
            }
        }
        val opened = mediaRepository.getVideoByUri(request.uri)
            ?: return listOf(mediaItemFor(request.uri, request.title, null))
        return getSortedVideos(opened.parentPath).first().map { video ->
            // The caller's name for the file it asked for beats the library's. It went to the
            // trouble of sending one, and it may know the film by something the file is not called.
            val title = request.title.takeIf { video.uriString == request.uri } ?: video.displayName
            mediaItemFor(video.uriString, title, video.duration)
        }
    }

    /** Whether a television is selected right now. Asked of the router, which is what picked it. */
    private fun isCasting(): Boolean = runCatching {
        MediaRouter.getInstance(context).selectedRoute.isReceiver
    }.getOrDefault(false)

    private suspend fun titleFor(uri: String): String? = mediaRepository.getVideoByUri(uri)?.displayName

    /**
     * [durationMs] is carried on the item so that a file leaving the screen can be told apart from
     * one that finished. The player only reports the duration of what it is playing now.
     */
    private fun mediaItemFor(uri: String, title: String?, durationMs: Long?, isLive: Boolean = false): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            // Said out loud for a channel, because half of them are an address with no extension on
            // the end and nothing downstream can tell what it is by looking. The local player sniffs
            // the stream and does not care; a receiver is handed a type and refuses what it cannot
            // name -- and then steps to the next item in the queue, which on a channel list is the
            // next channel.
            .setMimeType(MimeTypes.APPLICATION_M3U8.takeIf { isLive })
            // The library's key for this file, so a position written here is found again on
            // reopening.
            .setMediaId(uri)
            // Handed to the item rather than kept beside it, so that everything reading the player
            // sees one name: the bar over the film, and later the floating window and the
            // notification.
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    // Said out loud, because one session carries both a film and a song, and what
                    // is in it is the only way anything downstream can tell which. The music bar
                    // over the library reads this to know it is not being asked to show a film.
                    .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
                    .setDurationMs(durationMs)
                    .build(),
            )
            .build()

    /**
     * Writes down where the film is, for the next time it is opened.
     *
     * Called by the screen as it goes away as well as on every pause, because a viewer who leaves
     * mid-scene never pauses first, and the player is released without saying so.
     */
    fun savePosition() {
        val player = controller ?: return
        val current = player.currentMediaItem ?: return
        // A channel has nowhere to be left. Writing one down means the next opening asks to start
        // there, which is meaningless on a broadcast and refused outright when it is being cast.
        if (player.isCurrentMediaItemLive) return
        // Read here and not in the coroutine: the player may only be asked from the thread that
        // owns it, and by the time the write runs this screen may be gone.
        write(current.mediaId, player.currentPosition, player.duration)
    }

    private fun write(uri: String, position: Long, duration: Long?) {
        // A film watched to its last seconds is a film watched. Keeping that position would reopen
        // it on the credits and stop, which is never what anyone wants.
        val finished = duration != null && duration > 0L && position >= duration - EndOfFileSlackMs
        applicationScope.launch {
            mediaRepository.updateMediumPosition(uri, if (finished) StartOfFile else position)
        }
    }

    /**
     * How far into the picture this file was left. Written on the end of a pinch and not on every
     * frame of one, which would be a database row per finger movement.
     */
    fun saveZoom(zoom: Float) {
        val uri = controller?.currentMediaItem?.mediaId ?: return
        applicationScope.launch { mediaRepository.updateMediumZoom(uri, zoom) }
    }

    var onlineSubtitles: OnlineSubtitleState by mutableStateOf(OnlineSubtitleState.Idle)
        private set

    /** Everything the viewer has set for playback, as it stands. Written straight through, since
     *  each change is judged by looking at what it did. */
    val preferences = preferencesRepository.playerPreferences

    fun setSubtitleStyle(style: PlayerPreferences) {
        applicationScope.launch { preferencesRepository.updatePlayerPreferences { style } }
    }

    /** A change to one field, for callers that hold no copy of the rest. */
    fun updatePreferences(transform: PlayerPreferences.() -> PlayerPreferences) {
        applicationScope.launch { preferencesRepository.updatePlayerPreferences { it.transform() } }
    }

    /**
     * Looks for a subtitle on OpenSubtitles, by the file's own fingerprint first.
     *
     * That fingerprint is the size and two 64KB chunks, and it is what matches a *release* rather
     * than a title: the same film cut two ways needs two different subtitle timings, and only the
     * hash can tell them apart. A search by name is the fallback, for a file renamed past
     * recognition or one the site has never seen.
     */
    fun searchSubtitles(query: String? = null, languageId: String = "") {
        val current = controller?.currentMediaItem ?: return
        onlineSubtitles = OnlineSubtitleState.Searching
        viewModelScope.launch {
            val found = when {
                query != null -> openSubtitles.searchByQuery(query, languageId)
                else -> searchByFingerprint(current.mediaId) ?: return@launch
            }
            onlineSubtitles = found.fold(
                onSuccess = { OnlineSubtitleState.Found(it) },
                onFailure = { OnlineSubtitleState.Failed },
            )
        }
    }

    private suspend fun searchByFingerprint(uri: String): Result<List<OpenSubtitleResult>>? {
        val path = mediaRepository.getVideoByUri(uri)?.path
        val hashed = path?.let { OpenSubtitlesHasher.computeHash(it) }
        if (hashed == null) {
            // No path to hash, which is every file handed over by a provider that keeps its own.
            // Fall back to the name, since that is all there is.
            val name = current(uri) ?: return null.also { onlineSubtitles = OnlineSubtitleState.Failed }
            return openSubtitles.searchByQuery(name)
        }
        val (hash, size) = hashed
        return openSubtitles.searchByHash(hash, size)
    }

    private suspend fun current(uri: String): String? = mediaRepository.getVideoByUri(uri)?.displayName

    /** Fetches one of the results and puts it on the film. */
    fun downloadSubtitle(result: OpenSubtitleResult) {
        val shown = onlineSubtitles as? OnlineSubtitleState.Found ?: return
        onlineSubtitles = OnlineSubtitleState.Downloading(shown.results, result)
        viewModelScope.launch {
            val file = openSubtitles.downloadSubtitle(result, context.cacheDir).getOrNull()
            if (file == null) {
                onlineSubtitles = OnlineSubtitleState.Failed
                return@launch
            }
            addSubtitle(Uri.fromFile(file))
            onlineSubtitles = shown
        }
    }

    /** A subtitle the viewer went and found, for a film that shipped without one. */
    fun addSubtitle(uri: Uri) {
        val player = controller ?: return
        viewModelScope.launch { player.addSubtitle(context.externalSubtitle(uri)) }
    }

    /**
     * What this file remembers, applied: where it was left, and how far its captions were shifted.
     *
     * One read for both. They come from the same row, and asking for it twice on every change of
     * item is a second query answering a question the first already answered.
     */
    private fun onItemOpened(uri: String) {
        viewModelScope.launch {
            val saved = mediaRepository.getVideoState(uri)
            applySubtitleDelay(saved?.subtitleDelayMilliseconds ?: NoSubtitleDelay)

            if (preferencesRepository.playerPreferences.value.resume == Resume.NO) return@launch
            val position = saved?.position ?: return@launch
            if (position <= StartOfFile) return@launch
            // The queue may have moved on again while this was being read.
            val player = controller ?: return@launch
            if (player.currentMediaItem?.mediaId == uri) player.seekTo(position)
        }
    }

    /**
     * Sent without being written back, for a value that came out of the store in the first place.
     *
     * Always sent, zero included: the offset belongs to the renderer rather than to the film, so a
     * file opened after one that was shifted inherits the shift unless it is told otherwise.
     */
    private fun applySubtitleDelay(millis: Long) {
        subtitleDelayMs = millis
        // The viewer's own shift, less the window the translation needs. Kept apart on purpose:
        // what is remembered for the file is what the viewer chose, and the lookahead is a working
        // offset that belongs to the translation being on -- storing the two added together would
        // leave every translated file two seconds out the next time it was opened untranslated.
        val sent = millis - if (isTranslating) TranslationLookaheadMs else 0L
        controller?.setSubtitleDelay(sent)
    }

    /**
     * Whether the captions are being translated as they arrive.
     *
     * Held here because it changes what the renderer is asked for: translating pulls every line
     * forward by [TranslationLookaheadMs] so the round trip happens before the line is due. Turning
     * it off puts the shift back, or the captions would run early for the rest of the film.
     */
    private var isTranslating = false
    private var lookaheadJob: Job? = null

    fun setTranslating(enabled: Boolean) {
        if (enabled == isTranslating) return
        isTranslating = enabled
        lookaheadJob?.cancel()
        applySubtitleDelay(subtitleDelayMs)
        if (!enabled) return

        // Said again on a timer, as the old player says it.
        //
        // The offset belongs to the text renderer, and anything that rebuilds one drops it -- a
        // track chosen, a subtitle added, a seek that resets it. Sent once, the lookahead survives
        // until the first of those and then the captions are late again with nothing on screen to
        // say why. Six commands a minute is nothing next to being wrong for the rest of the film.
        lookaheadJob = viewModelScope.launch {
            while (true) {
                delay(LookaheadRefreshMs)
                applySubtitleDelay(subtitleDelayMs)
            }
        }
    }

    /**
     * How far the captions are shifted against the sound, in milliseconds.
     *
     * Held here rather than read back from the service: this is the one place that already knows
     * the value, having just loaded it, and a round trip through the session to learn what we sent
     * would be a query for our own answer.
     */
    var subtitleDelayMs: Long by mutableStateOf(NoSubtitleDelay)
        private set

    private var subtitleDelayWrite: Job? = null

    /**
     * The shift waiting to be written down, and the file it belongs to.
     *
     * The pair and not the two separately: the loop below settles on what it finds here, and if it
     * read the live [subtitleDelayMs] instead it would pick up the *next* film's value when the
     * queue moves on mid-wait -- and write that against the file that is leaving, erasing what was
     * just set on it.
     */
    private var pendingSubtitleDelay: Pair<String, Long>? = null

    /**
     * Shift the captions, and remember it for this file.
     *
     * The renderer hears every step, because the whole point is watching the line land; the store
     * hears one, once the stepper has been still for a moment. Held down, this fires dozens of
     * times a second, and a write per step is a write per frame of a gesture nobody has finished.
     *
     * One coroutine for the whole gesture rather than one cancelled and replaced per step: it waits
     * out the quiet period, and if the value moved again while it waited, it waits again.
     */
    fun setSubtitleDelay(millis: Long) {
        val wanted = millis.coerceIn(-MaxSubtitleDelayMs, MaxSubtitleDelayMs)
        if (wanted == subtitleDelayMs) return
        applySubtitleDelay(wanted)

        val uri = controller?.currentMediaItem?.mediaId ?: return
        pendingSubtitleDelay = uri to wanted
        if (subtitleDelayWrite?.isActive == true) return
        subtitleDelayWrite = applicationScope.launch {
            var settled: Pair<String, Long>?
            do {
                settled = pendingSubtitleDelay
                delay(SubtitleDelaySettleMs)
            } while (settled != pendingSubtitleDelay)
            settled?.let { (file, shift) -> mediaRepository.updateSubtitleDelay(file, shift) }
        }
    }

    /**
     * The night-mode limiter, on the session rather than on this screen.
     *
     * Saved as well as sent: the effect lives on an audio session that is rebuilt whenever the
     * player is, and what survives that is the preference the service reads on binding.
     */
    /**
     * A drag is under way on the seek bar, or has ended. See [dev.vayou.core.player.setScrubbing].
     *
     * Sent only when it changes. The bar reports the end of every gesture, tap included, because the
     * music screen needs that to commit -- and here a tap would otherwise leave keyframe mode it was
     * never in.
     */
    private var isScrubbing = false

    fun setScrubbing(isScrubbing: Boolean) {
        if (isScrubbing == this.isScrubbing) return
        this.isScrubbing = isScrubbing
        controller?.setScrubbing(isScrubbing)
    }

    fun setNightMode(enabled: Boolean) {
        controller?.setNightMode(enabled)
        updatePreferences { copy(nightModeEnabled = enabled) }
    }

    /**
     * Stops for good, as against merely leaving the screen.
     *
     * The service outlives this screen on purpose, so something has to say when the film is over
     * rather than merely out of sight. Closing the player means closing the film -- unless the
     * viewer has asked for the sound to carry on, which is what a lecture in a pocket is.
     *
     * [keepPlaying] is them saying so on the way out, where [PlayerPreferences.autoBackgroundPlay]
     * is them having said so in advance. Either answers the same question.
     */
    fun stopPlayback(keepPlaying: Boolean = false) {
        // Emptied, not merely paused. The service outlives this screen, and a film left loaded in
        // it is a film the notification goes on offering and the library's music bar goes on
        // showing -- there is one session, and what is in it is what the rest of the app sees.
        //
        // The position is already written: the activity saves it on stop, which runs before this.
        if (!keepPlaying && !preferencesRepository.playerPreferences.value.autoBackgroundPlay) {
            controller?.run {
                clearMediaItems()
                stop()
            }
        }
        openedUri = null
    }

    /**
     * Lets go of the connection, not of what is playing.
     *
     * Releasing the controller leaves the service and its player where they are, which is the whole
     * point of moving them there. The service decides for itself when there is nothing left to do.
     */
    override fun onCleared() {
        controller?.run {
            removeListener(listener)
            release()
        }
        controller = null
    }
}

private const val StartOfFile = 0L

/** Captions on the sound, which is where a file starts. */
private const val NoSubtitleDelay = 0L

/**
 * Ten seconds either way. Past that a caption is not out of sync with the film -- it is the wrong
 * subtitle file, and no stepper is going to fix that.
 */
private const val MaxSubtitleDelayMs = 10_000L

/** How long the stepper has to be still before what it settled on is written down. */
private const val SubtitleDelaySettleMs = 500L

/** Credits, a fade, a couple of seconds of black -- close enough to the end to count as the end. */
/** How often the lookahead is said again while translating. Ten seconds, as the old player uses. */
private const val LookaheadRefreshMs = 10_000L

private const val EndOfFileSlackMs = 3_000L
