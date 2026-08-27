package dev.vayou.tv.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.domain.GetSortedVideosUseCase
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
     * before it finishes. Written outside cancellation, because this runs as the screen closes and
     * the scope is being torn down around it.
     */
    fun saveProgress(positionMs: Long, durationMs: Long) {
        // A channel has no position to come back to, and its "duration" is wherever the live edge
        // happened to be.
        if (isLive || durationMs <= 0L) return
        val position = if (positionMs >= durationMs - CompletionSlackMs) StartOfFile else positionMs.coerceAtLeast(0L)
        viewModelScope.launch {
            withContext(NonCancellable) {
                mediaRepository.updateMediumPosition(videoUri, position)
                mediaRepository.updateMediumLastPlayedTime(videoUri, System.currentTimeMillis())
            }
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
