package dev.vayou.core.domain

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.common.Dispatcher
import dev.vayou.core.common.VayouDispatchers
import dev.vayou.core.common.extensions.getPath
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.MediaViewMode
import dev.vayou.core.model.Video
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GetSortedPlaylistUseCase @Inject constructor(
    private val getSortedVideosUseCase: GetSortedVideosUseCase,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context,
    @Dispatcher(VayouDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(uri: Uri): List<Video> = withContext(defaultDispatcher) {
        // Local-only feature: only `file://` and `content://` are indexed in the local Room DB.
        // For SMB / HTTP / IPTV URIs the lookup always returns empty, so skip the work entirely.
        val scheme = uri.scheme?.lowercase()
        if (scheme != "file" && scheme != "content") return@withContext emptyList()

        val path = context.getPath(uri) ?: return@withContext emptyList()
        val parent = File(path).parent.takeIf {
            preferencesRepository.applicationPreferences.first().mediaViewMode != MediaViewMode.VIDEOS
        }

        getSortedVideosUseCase.invoke(parent).first()
    }
}
