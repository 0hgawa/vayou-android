package dev.vayou.feature.music

import android.net.Uri
import androidx.annotation.StringRes
import dev.vayou.core.media.Song
import dev.vayou.core.model.MediaPlaylist
import dev.vayou.core.model.SmartPlaylist

/**
 * How the library is presented.
 *
 * Every tab reads the same scan -- the grouping keys already come back on each [Song] -- so
 * switching costs a regroup in memory, never another MediaStore query.
 */
enum class MusicTab(@param:StringRes val label: Int) {
    Songs(R.string.tab_songs),
    Playlists(R.string.tab_playlists),
    Folders(R.string.tab_folders),
    Albums(R.string.tab_albums),
    Artists(R.string.tab_artists),
    ;

    /**
     * What this tab groups by, or null where the grouping is not read off a track's own tags: the
     * flat list, and playlists, whose membership is stored rather than tagged.
     */
    fun keyOf(song: Song): String? = when (this) {
        Songs, Playlists -> null
        Albums -> song.album
        Artists -> song.artist
        Folders -> song.folderPath
    }
}

/** One row of a grouped tab: an album, an artist or a folder, and the tracks under it. */
data class MusicGroup(
    /** Identity, and what the group is keyed and searched by. */
    val key: String,
    /** What the row shows. A folder displays its own name, not the whole path. */
    val label: String,
    val songs: List<Song>,
    /** The first cover found among the tracks; a group of untagged tracks has none. */
    val artworkUri: Uri?,
)

/**
 * [songs] grouped for [tab], keeping the incoming order inside each group so the chosen sort still
 * applies once a group is opened. Empty for the two tabs that have no groups.
 */
/**
 * The listener's own lists, as groups.
 *
 * Built here rather than by [groupSongs] because a playlist is not a property of a track: the other
 * tabs read a tag off each file and gather what matches, while this reads a list the listener wrote
 * and looks the tracks up by address. Its order is theirs, so it is not sorted.
 */
/**
 * Starred, as a list among the lists.
 *
 * Not one of the listener's own -- it is the list every library has and nobody made -- so it is
 * built here rather than stored, and it is kept at the head wherever the others are ordered: it is
 * the one opened daily, and the only one there on a fresh install.
 */
fun favouritesGroup(favourites: List<Song>, label: String): MusicGroup = MusicGroup(
    key = SmartPlaylist.Favourites,
    label = label,
    songs = favourites,
    artworkUri = null,
)

fun playlistGroups(playlists: List<MediaPlaylist>, songs: List<Song>): List<MusicGroup> {
    val byUri = songs.associateBy { it.uriString }
    return playlists.map { playlist ->
        val tracks = playlist.itemUris.mapNotNull(byUri::get)
        MusicGroup(
            key = playlist.id,
            label = playlist.name,
            songs = tracks,
            artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
        )
    }
}

fun groupSongs(songs: List<Song>, tab: MusicTab, unknownLabel: String): List<MusicGroup> {
    if (tab == MusicTab.Songs || tab == MusicTab.Playlists) return emptyList()
    return songs
        .groupBy { tab.keyOf(it).orEmpty() }
        .map { (key, tracks) ->
            MusicGroup(
                key = key,
                label = when {
                    key.isBlank() -> unknownLabel
                    tab == MusicTab.Folders -> key.substringAfterLast('/').ifBlank { key }
                    else -> key
                },
                songs = tracks,
                artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
