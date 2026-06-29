package dev.vayou.core.domain

import dev.vayou.core.common.Dispatcher
import dev.vayou.core.common.VayouDispatchers
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.Folder
import dev.vayou.core.model.MediaViewMode
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class GetSortedMediaUseCase @Inject constructor(
    private val getSortedVideosUseCase: GetSortedVideosUseCase,
    private val getSortedFoldersUseCase: GetSortedFoldersUseCase,
    private val preferencesRepository: PreferencesRepository,
    @Dispatcher(VayouDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(folderPath: String? = null): Flow<Folder?> {
        return combine(
            getSortedVideosUseCase(folderPath),
            getSortedFoldersUseCase(),
            preferencesRepository.applicationPreferences,
        ) { videos, folders, preferences ->
            when (preferences.mediaViewMode) {
                MediaViewMode.FOLDERS -> if (folderPath == null) {
                    Folder.rootFolder.copy(
                        mediaList = emptyList(),
                        folderList = folders,
                    )
                } else {
                    val file = File(folderPath)
                    Folder(
                        name = file.name,
                        path = file.path,
                        dateModified = file.lastModified(),
                        mediaList = videos,
                        folderList = emptyList(),
                    )
                }
                MediaViewMode.VIDEOS -> Folder.rootFolder.copy(
                    mediaList = videos,
                    folderList = emptyList(),
                )
            }
        }.flowOn(defaultDispatcher)
    }
}
