package dev.vayou.core.smb

import kotlinx.serialization.Serializable

data class SmbServer(
    val host: String,
    val displayName: String = host,
)

data class SmbShare(
    val name: String,
)

data class SmbFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
) {
    val isVideo: Boolean get() = name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS
    val isSubtitle: Boolean get() = name.substringAfterLast('.', "").lowercase() in SUBTITLE_EXTENSIONS
    fun isSubtitleFor(videoBase: String): Boolean {
        if (!isSubtitle) return false
        val base = name.substringBeforeLast('.')
        return base.equals(videoBase, ignoreCase = true) ||
            base.startsWith("$videoBase.", ignoreCase = true)
    }
}

@Serializable
data class PlaylistChannel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
)

@Serializable
data class FavoriteFolder(
    val host: String,
    val share: String,
    val path: String,
    val displayName: String,
)

data class SmbCredentials(
    val username: String = "",
    val password: String = "",
    val domain: String = "",
)

data class SavedSmbServer(
    val host: String,
    val displayName: String,
    val username: String,
    val domain: String,
)

data class NetworkServerEntry(
    val host: String,
    val displayName: String,
    val isSaved: Boolean,
    val isOnline: Boolean,
)

fun mergeNetworkServers(
    saved: List<SavedSmbServer>,
    discovered: List<SmbServer>,
): List<NetworkServerEntry> {
    val savedByHost = saved.associateBy { it.host }
    val discoveredByHost = discovered.associateBy { it.host }
    return (savedByHost.keys + discoveredByHost.keys).map { host ->
        val s = savedByHost[host]
        val d = discoveredByHost[host]
        NetworkServerEntry(
            host = host,
            displayName = s?.displayName ?: d?.displayName ?: host,
            isSaved = s != null,
            isOnline = d != null,
        )
    }
}

@Serializable
internal data class StoredSmbServer(
    val host: String,
    val displayName: String,
    val username: String,
    val password: String,
    val domain: String,
)

internal fun StoredSmbServer.toSaved() = SavedSmbServer(
    host = host,
    displayName = displayName,
    username = username,
    domain = domain,
)

internal fun StoredSmbServer.toCredentials() = SmbCredentials(
    username = username,
    password = password,
    domain = domain,
)

private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
    "mpg", "mpeg", "3gp", "ts", "mts", "m2ts", "vob", "ogv",
)

private val SUBTITLE_EXTENSIONS = setOf("srt", "ssa", "ass", "vtt", "ttml")
