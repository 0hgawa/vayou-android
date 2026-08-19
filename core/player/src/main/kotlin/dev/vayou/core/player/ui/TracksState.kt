package dev.vayou.core.player.ui

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import java.util.Locale

/** One track the file carries, as a row in a list. */
data class MediaTrack(
    val label: String,
    val isSelected: Boolean,
    private val group: Tracks.Group,
    private val indexInGroup: Int,
) {
    internal fun asOverride() = TrackSelectionOverride(group.mediaTrackGroup, indexInGroup)
}

/**
 * The tracks of one kind the file carries, and which is on.
 *
 * One class for audio and for text, because the difference between them is a constant. What differs
 * is what the absence means: a file with no audio selected is silent by mistake, one with no
 * subtitle selected is the normal case, and that is the caller's to decide.
 */
@UnstableApi
@Stable
@OptIn(UnstableApi::class)
class TracksState(private val player: Player, private val trackType: @C.TrackType Int) {

    var tracks: List<MediaTrack> by mutableStateOf(emptyList())
        private set

    val isOff: Boolean get() = tracks.none { it.isSelected }

    suspend fun observe() {
        read()
        player.listen { events ->
            if (events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_TRACK_SELECTION_PARAMETERS_CHANGED)) {
                read()
            }
        }
    }

    fun select(track: MediaTrack) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(track.asOverride())
            .build()
    }

    /** Disabled rather than cleared: clearing lets the selector pick one again at the next change,
     *  so "off" would not stay off. */
    fun turnOff() {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    }

    private fun read() {
        tracks = player.currentTracks.groups
            .filter { it.type == trackType }
            .flatMap { group ->
                (0 until group.length).map { index ->
                    MediaTrack(
                        label = group.getTrackFormat(index).trackLabel(index),
                        isSelected = group.isTrackSelected(index),
                        group = group,
                        indexInGroup = index,
                    )
                }
            }
    }
}

/**
 * What to call a track in a list you pick from: "German", "English · SDH", "Korean (SRT)".
 *
 * The language first and in full, because that is what anyone scanning the list is looking for. The
 * embedded label after it, and only when it says something the language does not -- half the files
 * in the world label their German track "German". Then the format in brackets, which is how every
 * desktop player has always written it and the only thing that tells a picture-based track from a
 * text one.
 *
 * The language is read with [Locale.forLanguageTag] and not `Locale(code)`, whose `toString` is the
 * code again -- which is why this list was a column of "de", "en", "pt_BR". It reads region tags
 * too, so "pt-BR" arrives as Portuguese rather than as nothing.
 */
@OptIn(UnstableApi::class)
private fun Format.trackLabel(index: Int): String {
    val spoken = language
        ?.takeUnless { it == C.LANGUAGE_UNDETERMINED }
        ?.let { Locale.forLanguageTag(it).displayLanguage }
        ?.takeIf { it.isNotBlank() }
    val given = label?.takeIf { it.isNotBlank() }

    val base = when {
        spoken != null && given != null && !given.equals(spoken, ignoreCase = true) -> "$spoken · $given"
        spoken != null -> spoken
        given != null -> given
        // Numbered from one, and without saying which kind: the list it sits in is already one kind.
        else -> "Track ${index + 1}"
    }
    // Media3 parses text tracks in the extractor now, so a subtitle's sampleMimeType is the container
    // it hands cues over in, and what it was written in has moved to `codecs`. Reading only the
    // first, every embedded subtitle came out with no tag at all.
    val tag = (sampleMimeType?.takeUnless { it == MimeTypes.APPLICATION_MEDIA3_CUES } ?: codecs)
        ?.let(::formatTag)
        // An external subtitle is named by its file, and that name ends in the extension already:
        // "legenda.pt-BR.srt (SRT)" says it twice.
        ?.takeUnless { base.endsWith(".$it", ignoreCase = true) }
    return if (tag != null) "$base ($tag)" else base
}

/**
 * The short name for a codec, as a player writes it: SRT, PGS, AC3.
 *
 * Only for the ones worth naming. A tag nobody recognises is noise in a row that already says the
 * language, so anything unmapped adds nothing.
 */
@OptIn(UnstableApi::class)
private fun formatTag(mimeType: String): String? = when (mimeType) {
    MimeTypes.APPLICATION_SUBRIP -> "SRT"
    MimeTypes.TEXT_SSA -> "ASS"
    MimeTypes.TEXT_VTT, MimeTypes.APPLICATION_MP4VTT -> "VTT"
    MimeTypes.APPLICATION_PGS -> "PGS"
    MimeTypes.APPLICATION_VOBSUB -> "VobSub"
    MimeTypes.APPLICATION_DVBSUBS -> "DVB"
    MimeTypes.APPLICATION_TTML -> "TTML"
    MimeTypes.APPLICATION_TX3G -> "TX3G"
    MimeTypes.AUDIO_AAC -> "AAC"
    MimeTypes.AUDIO_AC3 -> "AC3"
    MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3"
    MimeTypes.AUDIO_AC4 -> "AC4"
    MimeTypes.AUDIO_DTS -> "DTS"
    MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
    MimeTypes.AUDIO_TRUEHD -> "TrueHD"
    MimeTypes.AUDIO_FLAC -> "FLAC"
    MimeTypes.AUDIO_OPUS -> "Opus"
    MimeTypes.AUDIO_MPEG -> "MP3"
    MimeTypes.AUDIO_VORBIS -> "Vorbis"
    MimeTypes.AUDIO_RAW -> "PCM"
    else -> null
}

@UnstableApi
@Composable
fun rememberTracksState(player: Player, trackType: @C.TrackType Int): TracksState {
    val state = remember(player, trackType) { TracksState(player, trackType) }
    LaunchedEffect(state) { state.observe() }
    return state
}
