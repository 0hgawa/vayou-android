package dev.vayou.tv.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.model.Video
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FavoritesStore
import dev.vayou.core.smb.FolderFavoritesStore
import dev.vayou.core.smb.PlaylistStore
import dev.vayou.core.smb.RecentPlay
import dev.vayou.core.smb.RecentPlaysStore
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SavedSmbServer
import dev.vayou.core.smb.SmbServerStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MaxRecent = 20
private const val MaxRow = 16

@HiltViewModel
class HomeViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    smbServerStore: SmbServerStore,
    folderFavoritesStore: FolderFavoritesStore,
    private val playlistStore: PlaylistStore,
    recentPlaysStore: RecentPlaysStore,
    favoritesStore: FavoritesStore,
) : ViewModel() {

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch { playlistStore.add(name, url) }
    }

    fun removePlaylist(url: String) {
        viewModelScope.launch { playlistStore.remove(url) }
    }

    fun renamePlaylist(url: String, newName: String) {
        viewModelScope.launch { playlistStore.rename(url, newName) }
    }

    val state: StateFlow<HomeState> = combine(
        mediaRepository.getVideosFlow(),
        smbServerStore.savedServersFlow,
        folderFavoritesStore.favoritesFlow,
        playlistStore.playlistsFlow,
        recentPlaysStore.recentsFlow,
        favoritesStore.favoritesFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val videos = values[0] as List<Video>
        @Suppress("UNCHECKED_CAST")
        val servers = values[1] as List<SavedSmbServer>
        @Suppress("UNCHECKED_CAST")
        val folders = values[2] as List<FavoriteFolder>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[3] as List<SavedPlaylist>
        @Suppress("UNCHECKED_CAST")
        val externalRecents = values[4] as List<RecentPlay>
        @Suppress("UNCHECKED_CAST")
        val iptvFavs = values[5] as List<dev.vayou.core.smb.PlaylistChannel>

        val localItems = videos.asSequence()
            .filter { it.lastPlayedAt != null }
            .map { it.toRecentItem() }
        val externalItems = externalRecents.asSequence().map(RecentPlay::toRecentItem)
        HomeState(
            recent = (localItems + externalItems)
                .sortedByDescending { it.lastPlayedAt }
                .distinctBy { it.uri }
                .take(MaxRecent)
                .toList(),
            servers = servers.take(MaxRow),
            folderFavoritesCount = folders.size,
            playlists = playlists.take(MaxRow),
            iptvFavoritesCount = iptvFavs.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeState(),
    )

    init {
        viewModelScope.launch { playlistStore.seedDefaultsIfNeeded() }
    }
}

data class HomeState(
    val recent: List<RecentItem> = emptyList(),
    val servers: List<SavedSmbServer> = emptyList(),
    val folderFavoritesCount: Int = 0,
    val playlists: List<SavedPlaylist> = emptyList(),
    val iptvFavoritesCount: Int = 0,
)

enum class RecentType { LOCAL, SMB, STREAM }

data class RecentItem(
    val uri: String,
    val displayName: String,
    val durationLabel: String,
    val sizeLabel: String,
    val playedPercentage: Float,
    val thumbnailModel: Any?,
    val lastPlayedAt: Long,
    val type: RecentType,
)

private fun Video.toRecentItem() = RecentItem(
    uri = uriString,
    displayName = displayName,
    durationLabel = formattedDuration,
    sizeLabel = formattedFileSize,
    playedPercentage = playedPercentage,
    thumbnailModel = uriString,
    lastPlayedAt = lastPlayedAt?.time ?: 0L,
    type = RecentType.LOCAL,
)

private fun RecentPlay.toRecentItem() = RecentItem(
    uri = uri,
    displayName = displayName.substringBeforeLast('.').ifBlank { displayName },
    durationLabel = formatMillis(durationMs),
    sizeLabel = "",
    playedPercentage = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
    thumbnailModel = null,
    lastPlayedAt = lastPlayedAt,
    type = if (uri.startsWith("smb://")) RecentType.SMB else RecentType.STREAM,
)

private fun formatMillis(ms: Long): String {
    if (ms <= 0L) return ""
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
