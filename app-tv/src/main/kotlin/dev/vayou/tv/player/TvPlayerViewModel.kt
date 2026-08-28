package dev.vayou.tv.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.common.OpenSubtitlesHasher
import dev.vayou.core.common.di.ApplicationScope
import dev.vayou.core.data.models.OnlineSubtitleState
import dev.vayou.core.data.models.OpenSubtitleResult
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.OpenSubtitlesRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.domain.GetSortedVideosUseCase
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * What has to be known before a film can start, and what to write down when it stops.
 *
 * Both halves are here rather than in the screen because both outlive it: the position is asked
 * for before there is a surface to draw on, and it is saved as the screen is being taken apart.
 */
@HiltViewModel
class TvPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val getSortedVideos: GetSortedVideosUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val openSubtitles: OpenSubtitlesRepository,
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    /** How the captions are drawn, which is the viewer's to set and the same on either device. */
    val preferences: StateFlow<PlayerPreferences> = preferencesRepository.playerPreferences

    /**
     * As it arrived, and not decoded again.
     *
     * Encoded once to travel as a segment of a route and decoded once by navigation on the way in,
     * which leaves the address as it was. A second decode would undo the escaping a file's own name
     * needed -- a `#` in it opens the fragment of an address, and everything after would be dropped.
     */
    val videoUri: String = savedStateHandle[VideoUriArg] ?: error("Opened with no film to play")

    /** What the search has to say, from not asked yet through to a file on its way down. */
    var onlineSubtitles: OnlineSubtitleState by mutableStateOf(OnlineSubtitleState.Idle)
        private set

    /** Which language to ask for; blank is every one, which is where a viewer starts. */
    var subtitleLanguage: String by mutableStateOf("")
        private set

    /**
     * What to search for, starting from the file's own name.
     *
     * A name is a poor question on a television: what a share holds is called `S02E05 - Historia`
     * as often as it is called anything a subtitle site has heard of. So it is a starting point
     * rather than the whole of it, and the viewer can say what the film is actually called.
     */
    var subtitleQuery: String by mutableStateOf(searchTerm())
        private set

    /**
     * Looks for a subtitle, by the file's own fingerprint where there is a file to fingerprint.
     *
     * That fingerprint is the size and two 64KB chunks, and it matches a *release* rather than a
     * title: the same film cut two ways needs two different timings, and only the hash tells them
     * apart. It needs a path on this machine, which a film on a share does not have -- so the name
     * is the fallback, and on a television it is the usual case rather than the exception.
     */
    fun searchSubtitles() {
        onlineSubtitles = OnlineSubtitleState.Searching
        viewModelScope.launch {
            // Only while the question is still the file itself. Once the viewer has said what the
            // film is called, they have told us the name matters more than the bytes -- and the
            // hash would go on answering the question they just replaced.
            val hashed = if (subtitleQuery != searchTerm()) {
                null
            } else {
                mediaRepository.getVideoByUri(videoUri)?.path?.let { OpenSubtitlesHasher.computeHash(it) }
            }
            val found = if (hashed == null) {
                openSubtitles.searchByQuery(subtitleQuery, subtitleLanguage)
            } else {
                openSubtitles.searchByHash(hashed.first, hashed.second, subtitleLanguage)
            }
            onlineSubtitles = found.fold(
                onSuccess = { OnlineSubtitleState.Found(it) },
                onFailure = { OnlineSubtitleState.Failed },
            )
        }
    }

    /** The name the file goes by, which is all there is to search on for anything off a share. */
    private fun searchTerm(): String = Uri.decode(videoUri.substringAfterLast('/')).substringBeforeLast('.')

    /** Asked again under another name, which is the viewer correcting what the file is called. */
    fun searchSubtitlesFor(query: String) {
        subtitleQuery = query.trim().ifBlank { searchTerm() }
        searchSubtitles()
    }

    /** Asking again in another language, which is a different question about the same film. */
    fun chooseSubtitleLanguage(id: String) {
        subtitleLanguage = id
        searchSubtitles()
    }

    /**
     * Fetches one of them and hands back the file, for the screen to put on the film.
     *
     * The list stays up behind it: a viewer who picked the wrong release wants the next one, not
     * the search again.
     */
    fun downloadSubtitle(result: OpenSubtitleResult, onReady: (Uri) -> Unit) {
        val shown = onlineSubtitles as? OnlineSubtitleState.Found ?: return
        onlineSubtitles = OnlineSubtitleState.Downloading(shown.results, result)
        viewModelScope.launch {
            val file = openSubtitles.downloadSubtitle(result, context.cacheDir).getOrNull()
            if (file == null) {
                onlineSubtitles = OnlineSubtitleState.Failed
                return@launch
            }
            onlineSubtitles = shown
            onReady(Uri.fromFile(file))
        }
    }

    /**
     * Whether this is a channel, known before a single byte has arrived.
     *
     * The player can only read it off the manifest, which lands a second or two in -- long enough
     * for a viewer to watch the controls rearrange themselves from a film into a channel.
     */
    val isLive: Boolean = savedStateHandle[IsLiveArg] ?: false

    /**
     * Asked for from the film's own menu: play it again rather than carry on.
     *
     * Travels in the address rather than being read from the library, because it is not a fact
     * about the film -- it is what the viewer just chose, and the position they are ignoring has to
     * still be there if they change their mind halfway through.
     */
    private val isFromStart: Boolean = savedStateHandle[FromStartArg] ?: false

    private val _opening = MutableStateFlow<Opening?>(null)

    /** Null until both answers are in. The screen shows black rather than a half-built player. */
    val opening: StateFlow<Opening?> = _opening.asStateFlow()

    init {
        viewModelScope.launch {
            // A channel is in no library, has no position to resume and no folder to be one of: the
            // questions below are all about a film, and asking them of a channel is a wait for
            // three answers that are already known to be nothing.
            if (isLive) {
                _opening.value = Opening(startPosition = StartOfFile, queue = emptyList())
                return@launch
            }
            // The library is asked where the film is, rather than the address being read for it: a
            // film arrives here as `content://media/external/video/media/1000261638`, which has a
            // path with no folder anywhere in it. Taking one out of it gave an empty queue, and the
            // player fell back to naming the film after the number on the end of its address.
            val opened = mediaRepository.getVideoByUri(videoUri)
            val queue = opened?.parentPath
                ?.let { folder -> runCatching { getSortedVideos(folder).first() }.getOrNull() }
                .orEmpty()
            // The library first, and the table of positions behind it. A film on a share has no
            // library entry -- MediaStore never saw it -- so asking the library alone found nothing
            // and every one of them began again from the start, however far in the viewer had got.
            // The position was there all along: it is written down by address, and a share's
            // address is as good an address as any.
            val saved = opened?.playbackPosition
                ?: mediaRepository.getVideoState(videoUri)?.position
            val resumeAt = if (isFromStart) StartOfFile else saved ?: StartOfFile
            _opening.value = Opening(startPosition = resumeAt, queue = queue)
        }
    }

    /**
     * Where the viewer left the film, unless they were at the end of it.
     *
     * A film watched to the credits is a film to start again, not one to resume three seconds
     * before it finishes.
     *
     * On the application's own scope and not this model's, as the phone's player does it. This runs
     * as the screen is going away, and a coroutine started on a scope that has already been closed
     * never runs at all -- the position would be lost exactly on the exits that matter, which is
     * every one where the model is cleared before the composition is disposed.
     */
    fun saveProgress(positionMs: Long, durationMs: Long) {
        // A channel has no position to come back to, and its "duration" is wherever the live edge
        // happened to be.
        if (isLive || durationMs <= 0L) return
        val position = if (positionMs >= durationMs - CompletionSlackMs) StartOfFile else positionMs.coerceAtLeast(0L)
        applicationScope.launch {
            // The length goes down with the position. It is known here and nowhere else for a film
            // on a share, and without it no card can say how far in the viewer got.
            mediaRepository.updateMediumProgress(videoUri, position, durationMs)
            mediaRepository.updateMediumLastPlayedTime(videoUri, System.currentTimeMillis())
        }
    }

    /** How the captions look, which outlives the film and so is written to the store. */
    fun updatePreferences(update: (PlayerPreferences) -> PlayerPreferences) {
        viewModelScope.launch { preferencesRepository.updatePlayerPreferences(update) }
    }

    /** The films either side of this one, so the skip buttons have somewhere to go. */
    data class Opening(val startPosition: Long, val queue: List<Video>)

    companion object {
        const val VideoUriArg = "videoUri"

        const val IsLiveArg = "live"

        const val FromStartArg = "start"
    }
}

private const val StartOfFile = 0L

/** Nearer the end than this and the film counts as watched. */
private const val CompletionSlackMs = 5_000L
