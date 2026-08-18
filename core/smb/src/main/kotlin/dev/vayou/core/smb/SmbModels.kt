package dev.vayou.core.smb

import kotlinx.serialization.Serializable

data class SmbServer(val host: String, val displayName: String = host)

@JvmInline
value class SmbShare(val name: String)

data class SmbFileItem(val name: String, val path: String, val isDirectory: Boolean, val size: Long = 0) {
    /** Lower case and without the dot; empty for a directory and for a file that has none. */
    val extension: String get() = name.substringAfterLast('.', "").lowercase()

    val isVideo: Boolean get() = extension in VideoExtensions
    val isAudio: Boolean get() = extension in AudioExtensions
    val isSubtitle: Boolean get() = extension in SubtitleExtensions

    /**
     * Opens in the player. What the browser draws at full strength and acts on when tapped -- a
     * share also holds covers, subtitles and notes, which do neither.
     */
    val isPlayable: Boolean get() = isVideo || isAudio

    /** `Sintel.srt` and `Sintel.en.srt` both belong to `Sintel.mkv`. */
    fun isSubtitleFor(videoBaseName: String): Boolean {
        if (!isSubtitle) return false
        val base = name.substringBeforeLast('.')
        return base.equals(videoBaseName, ignoreCase = true) ||
            base.startsWith("$videoBaseName.", ignoreCase = true)
    }
}

@Serializable
data class PlaylistChannel(val name: String, val url: String, val logo: String? = null, val group: String? = null)

@Serializable
data class FavoriteFolder(val host: String, val share: String, val path: String, val displayName: String)

data class SmbCredentials(val username: String = "", val password: String = "", val domain: String = "")

/** A saved server as the screen sees it: everything but the password. */
data class SavedSmbServer(val host: String, val displayName: String, val username: String, val domain: String)

/**
 * A row in the server list. A server can be saved, found on the wire, or both -- and the row has to
 * say which, because a saved server that is not answering looks identical otherwise.
 */
data class NetworkServerEntry(val host: String, val displayName: String, val isSaved: Boolean, val isOnline: Boolean)

/** The saved list and the discovered list as one, keyed by address. */
fun mergeNetworkServers(saved: List<SavedSmbServer>, discovered: List<SmbServer>): List<NetworkServerEntry> {
    val savedByHost = saved.associateBy { it.host }
    val discoveredByHost = discovered.associateBy { it.host }
    return (savedByHost.keys + discoveredByHost.keys).map { host ->
        val stored = savedByHost[host]
        val found = discoveredByHost[host]
        NetworkServerEntry(
            host = host,
            displayName = stored?.displayName ?: found?.displayName ?: host,
            isSaved = stored != null,
            isOnline = found != null,
        )
    }
}

/** What goes to disk. The password never leaves this file's type. */
@Serializable
internal data class StoredSmbServer(
    val host: String,
    val displayName: String,
    val username: String,
    val password: String,
    val domain: String,
)

internal fun StoredSmbServer.toSaved() = SavedSmbServer(host, displayName, username, domain)

internal fun StoredSmbServer.toCredentials() = SmbCredentials(username, password, domain)

private val VideoExtensions = setOf(
    "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
    "mpg", "mpeg", "3gp", "ts", "mts", "m2ts", "vob", "ogv",
)

private val AudioExtensions = setOf(
    "mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "mka", "aiff", "alac",
)

private val SubtitleExtensions = setOf("srt", "ssa", "ass", "vtt", "ttml")
