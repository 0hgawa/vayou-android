package dev.vayou.core.domain

import dev.vayou.core.common.Dispatcher
import dev.vayou.core.common.VayouDispatchers
import dev.vayou.core.model.Folder
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * The library as one thing: every folder and every video, sorted, or the contents of one folder.
 *
 * It used to answer with only half of that -- folders or videos, whichever the stored view mode
 * asked for. That put a screen's tab choice in the data layer, which meant switching tabs was a
 * write to disk and a new query, and the library could not show the two side by side. Both lists
 * come back now and the screen renders the one its page is for; the stored mode is still what the
 * screen opens on, and is nothing more than that.
 */
class GetSortedMediaUseCase @Inject constructor(
    private val getSortedVideosUseCase: GetSortedVideosUseCase,
    private val getSortedFoldersUseCase: GetSortedFoldersUseCase,
    @Dispatcher(VayouDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(folderPath: String? = null): Flow<Folder?> = combine(
        getSortedVideosUseCase(folderPath),
        getSortedFoldersUseCase(),
    ) { videos, folders ->
        if (folderPath == null) {
            Folder.root.copy(mediaList = videos, folderList = folders)
        } else {
            // Inside a folder there is nothing to switch between: what is there is what it holds.
            val file = File(folderPath)
            Folder(
                name = file.name,
                path = file.path,
                dateModified = file.lastModified(),
                mediaList = videos,
                folderList = emptyList(),
            )
        }
    }.flowOn(defaultDispatcher)
}
