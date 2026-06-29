package dev.vayou.core.domain

import app.cash.turbine.test
import dev.vayou.core.data.repository.fake.FakeMediaRepository
import dev.vayou.core.data.repository.fake.FakePreferencesRepository
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Sort
import dev.vayou.core.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSortedFoldersUseCaseTest {

    private val mediaRepository = FakeMediaRepository()
    private val preferencesRepository = FakePreferencesRepository()
    private val useCase = GetSortedFoldersUseCase(
        mediaRepository = mediaRepository,
        preferencesRepository = preferencesRepository,
        defaultDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun emits_folders_sorted_by_title_asc() = runTest {
        mediaRepository.directories.addAll(testFolders.shuffled())
        preferencesRepository.updateApplicationPreferences {
            it.copy(sortBy = Sort.By.TITLE, sortOrder = Sort.Order.ASCENDING)
        }

        useCase().test {
            assertEquals(testFolders.sortedBy { it.name.lowercase() }, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun excludes_folders_listed_in_preferences() = runTest {
        mediaRepository.directories.addAll(testFolders)
        val excluded = testFolders.first().path
        preferencesRepository.updateApplicationPreferences {
            it.copy(excludeFolders = listOf(excluded))
        }

        useCase().test {
            val result = awaitItem()
            assertEquals(testFolders.size - 1, result.size)
            assertEquals(false, result.any { it.path == excluded })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun excludes_folders_with_empty_media() = runTest {
        val empty = Folder(name = "empty", path = "/empty", dateModified = 0L, mediaList = emptyList())
        mediaRepository.directories.add(empty)
        mediaRepository.directories.addAll(testFolders)

        useCase().test {
            val result = awaitItem()
            assertEquals(false, result.any { it.path == "/empty" })
            assertEquals(testFolders.size, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private val sampleVideo = Video(
    id = 1,
    duration = 1,
    uriString = "u",
    nameWithExtension = "v.mp4",
    width = 1,
    height = 1,
    path = "/a/v.mp4",
    size = 1,
)

private val testFolders = listOf(
    Folder(name = "Alpha", path = "/root/alpha", dateModified = 0L, mediaList = listOf(sampleVideo)),
    Folder(name = "charlie", path = "/root/charlie", dateModified = 0L, mediaList = listOf(sampleVideo)),
    Folder(name = "Bravo", path = "/root/bravo", dateModified = 0L, mediaList = listOf(sampleVideo)),
)
