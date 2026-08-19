package dev.vayou.feature.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.media.Lyrics
import dev.vayou.core.media.LyricsReader
import dev.vayou.core.media.MusicLibrary
import dev.vayou.core.media.Song
import dev.vayou.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val library: MusicLibrary,
    private val lyricsReader: LyricsReader,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    /** Backs the equalizer, which keeps its preset and its band gains the way the video player does. */
    val preferences: StateFlow<PlayerPreferences> = preferencesRepository.playerPreferences

    fun updatePreferences(transform: PlayerPreferences.() -> PlayerPreferences) {
        viewModelScope.launch { preferencesRepository.updatePlayerPreferences { it.transform() } }
    }

    /**
     * The library indexed by address, read once.
     *
     * The queue asks for this every time it changes -- a track added, one dragged to a new place --
     * and each of those would otherwise be a full MediaStore query to answer a question whose answer
     * has not moved.
     */
    private var indexed: Map<String, Song>? = null

    private val indexLock = Mutex()

    /**
     * [uris] as the store knows them, in the order given.
     *
     * Resolved here rather than carried in an intent: a queue of five hundred tracks would not fit
     * in a binder transaction, and the store already has every one of them.
     */
    suspend fun resolve(uris: List<String>): List<Song> = index().let { uris.mapNotNull(it::get) }

    /** The words of a track, when the file or the folder beside it has them. */
    suspend fun lyricsFor(song: Song): Lyrics? = lyricsReader.lyricsFor(song)

    /** The same lookup, keyed, for callers that ask about rows rather than about order. */
    suspend fun tracksFor(uris: List<String>): Map<String, Song> = index().filterKeys(uris.toHashSet()::contains)

    private suspend fun index(): Map<String, Song> = indexLock.withLock {
        indexed ?: library.all().associateBy { it.uriString }.also { indexed = it }
    }
}
