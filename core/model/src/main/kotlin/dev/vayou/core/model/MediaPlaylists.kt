package dev.vayou.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaLibrary { Music, Video }

/**
 * A film moved out of the public library into the app's own storage.
 *
 * Everything about it is written down here because the file has left MediaStore: nothing else on the
 * device knows how long it is, how big, or what it was called. A row cannot be drawn from the file
 * alone without opening it, and the point of the folder is that nothing opens it uninvited.
 */
@Serializable
data class PrivateVideo(
    /** Absolute path inside the app's own files directory, where no other app and no gallery looks. */
    val filePath: String,
    val name: String,
    val duration: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0L,
    val dateModified: Long = 0L,
)

/**
 * A named, ordered set of media, stored as addresses.
 *
 * Addresses rather than MediaStore ids, because a file deleted and re-added gets a new id and the
 * playlist should not silently pick up whatever inherited the old one.
 *
 * It says "items", not "tracks": one store serves both libraries, and half its entries are films.
 */
@Serializable
data class MediaPlaylist(
    val id: String,
    val name: String,
    val library: MediaLibrary,
    val itemUris: List<String> = emptyList(),
)

/**
 * Everything the viewer's own use produces: the lists they made, what they starred, and how often
 * each thing has been started.
 *
 * One document because all three are small, are read together by the same screen, and have to be
 * pruned together when files disappear.
 */
@Serializable
data class MediaPlaylists(
    /** Newest first, which is where a list just made is looked for. */
    val items: List<MediaPlaylist> = emptyList(),
    val favouriteUris: List<String> = emptyList(),
    /** Address to the number of times it has been started. */
    val playCounts: Map<String, Int> = emptyMap(),
    /** Newest first, so a film just hidden is at the top of the folder it went into. */
    val privateVideos: List<PrivateVideo> = emptyList(),
) {
    fun of(library: MediaLibrary): List<MediaPlaylist> = items.filter { it.library == library }

    fun toggleFavourite(uri: String): MediaPlaylists = copy(
        favouriteUris = if (uri in favouriteUris) favouriteUris - uri else listOf(uri) + favouriteUris,
    )

    fun recordPlay(uri: String): MediaPlaylists = copy(playCounts = playCounts + (uri to (playCounts[uri] ?: 0) + 1))

    fun add(id: String, name: String, library: MediaLibrary): MediaPlaylists =
        copy(items = listOf(MediaPlaylist(id, name, library)) + items)

    fun rename(id: String, name: String): MediaPlaylists = mapPlaylist(id) { it.copy(name = name) }

    fun remove(id: String): MediaPlaylists = copy(items = items.filterNot { it.id == id })

    /** Entries already in the list are skipped rather than duplicated. */
    fun addItems(id: String, uris: List<String>): MediaPlaylists = mapPlaylist(id) { playlist ->
        playlist.copy(itemUris = playlist.itemUris + uris.filterNot { it in playlist.itemUris })
    }

    fun removeItem(id: String, uri: String): MediaPlaylists =
        mapPlaylist(id) { playlist -> playlist.copy(itemUris = playlist.itemUris - uri) }

    /**
     * Called when media leaves the device, so nothing here points at a file that is gone.
     *
     * All three at once, which is the reason they share a document: a list pruned without its
     * favourites shrinks by one every time it is opened, and nobody would connect the two.
     */
    fun forgetItems(uris: Collection<String>): MediaPlaylists = copy(
        items = items.map { it.copy(itemUris = it.itemUris.filterNot { uri -> uri in uris }) },
        favouriteUris = favouriteUris.filterNot { it in uris },
        playCounts = playCounts.filterKeys { it !in uris },
    )

    /** Keyed on the path, so a second move of the same file replaces its record rather than doubling it. */
    fun addPrivate(video: PrivateVideo): MediaPlaylists =
        copy(privateVideos = listOf(video) + privateVideos.filterNot { it.filePath == video.filePath })

    fun removePrivate(filePaths: Collection<String>): MediaPlaylists =
        copy(privateVideos = privateVideos.filterNot { it.filePath in filePaths })

    private fun mapPlaylist(id: String, transform: (MediaPlaylist) -> MediaPlaylist) =
        copy(items = items.map { if (it.id == id) transform(it) else it })
}

/**
 * The lists a library derives rather than the viewer building them.
 *
 * Reserved strings and not a separate type, so they travel through the same open-and-close
 * machinery a real list does. Only the row that opens them and their lack of an edit menu differ.
 */
object SmartPlaylist {
    const val Favourites = "smart:favorites"

    const val MostPlayed = "smart:most_played"

    /** The video library's locked folder. */
    const val Private = "smart:private"

    /** How many "most played" holds; beyond this it stops being a shortlist. */
    const val MostPlayedLimit = 50

    fun isSmart(id: String): Boolean = id == Favourites || id == MostPlayed || id == Private
}
