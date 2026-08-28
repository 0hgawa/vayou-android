package dev.vayou.tv.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.media.Lyrics
import dev.vayou.core.media.LyricsReader
import dev.vayou.core.media.MusicLibrary
import dev.vayou.core.model.EqPreset
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the sleeve needs beyond the player: the words of the track, and which curve the sound is on.
 *
 * Kept out of [TvNowPlaying] because both answers come from disc and neither belongs to a
 * composition -- and kept out of [TvMusicViewModel] because the sleeve is opened from two screens,
 * the library and the network, and only one of them has that model.
 */
@HiltViewModel
class TvSleeveViewModel @Inject constructor(
    private val library: MusicLibrary,
    private val lyricsReader: LyricsReader,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _lyrics = MutableStateFlow<LyricsState>(LyricsState.Looking)

    /** The words of whatever is playing, or the reason there are none to show. */
    val lyrics: StateFlow<LyricsState> = _lyrics.asStateFlow()

    private val settings = preferencesRepository.playerPreferences

    /** Which curve is set, so the list can mark it without asking the player. */
    val preset: StateFlow<EqPreset> = settings
        .map { it.equalizerPreset }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.value.equalizerPreset)

    /** Whether the curve is being applied at all, which is what the "off" row turns. */
    val isEqualizerOn: StateFlow<Boolean> = settings
        .map { it.equalizerEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.value.equalizerEnabled)

    /**
     * Looks up the words for a track, by the address the player knows it as.
     *
     * A media id here is a MediaStore address for anything in the library and a URL for anything
     * streamed off the network, and only the first can be looked up -- a track coming down a wire
     * has no file beside it to hold an `.lrc` and no tag this side of the socket.
     */
    fun loadLyrics(mediaId: String?) {
        _lyrics.value = LyricsState.Looking
        val address = mediaId?.takeIf { it.startsWith("content://") } ?: run {
            _lyrics.value = LyricsState.None
            return
        }
        viewModelScope.launch {
            val song = library.byUri(address)
            val found = song?.let { lyricsReader.lyricsFor(it) }
            _lyrics.value = if (found == null) LyricsState.None else LyricsState.Found(found)
        }
    }

    /**
     * Remembers the curve, which is the half the player does not do.
     *
     * The sound itself changes through a session command the moment the row is chosen, because the
     * effects belong to the playback service. This is so the choice survives the screen closing.
     */
    fun rememberPreset(preset: EqPreset, isOn: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(equalizerPreset = preset, equalizerEnabled = isOn)
            }
        }
    }
}

/** Whether a track has words, said in a way a screen can draw without a second flag beside it. */
sealed interface LyricsState {
    /** Disc has not answered yet. A track with no words and a track not yet read look alike. */
    data object Looking : LyricsState

    data object None : LyricsState

    data class Found(val lyrics: Lyrics) : LyricsState
}
