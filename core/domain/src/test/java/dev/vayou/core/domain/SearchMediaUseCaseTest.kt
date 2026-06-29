package dev.vayou.core.domain

import app.cash.turbine.test
import dev.vayou.core.data.repository.fake.FakeMediaRepository
import dev.vayou.core.data.repository.fake.FakePreferencesRepository
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMediaUseCaseTest {

    private val mediaRepository = FakeMediaRepository()
    private val preferencesRepository = FakePreferencesRepository()
    private val dispatcher = Dispatchers.Unconfined
    private val sortedVideos = GetSortedVideosUseCase(mediaRepository, preferencesRepository, dispatcher)
    private val sortedFolders = GetSortedFoldersUseCase(mediaRepository, preferencesRepository, dispatcher)
    private val useCase = SearchMediaUseCase(sortedVideos, sortedFolders, dispatcher)

    @Test
    fun blank_query_returns_empty_results() = runTest {
        mediaRepository.videos.addAll(testVideos)

        useCase("   ").test {
            val result = awaitItem()
            assertTrue(result.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun single_word_matches_by_substring() = runTest {
        mediaRepository.videos.addAll(testVideos)

        useCase("endgame").test {
            val result = awaitItem()
            assertEquals(1, result.videos.size)
            assertEquals("Avengers Endgame 2019.mp4", result.videos.first().nameWithExtension)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun exact_phrase_ranks_above_out_of_order() = runTest {
        mediaRepository.videos.addAll(testVideos)

        useCase("stranger 2019").test {
            val hits = awaitItem().videos
            assertEquals("Stranger 2019.mp4", hits.first().nameWithExtension)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun words_in_order_with_gaps_still_match() = runTest {
        mediaRepository.videos.addAll(testVideos)

        useCase("stranger things").test {
            val hits = awaitItem().videos
            assertTrue(hits.any { it.nameWithExtension == "Stranger Things S01.mp4" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun out_of_order_words_still_match() = runTest {
        mediaRepository.videos.addAll(testVideos)

        useCase("2019 stranger").test {
            val hits = awaitItem().videos
            assertTrue(hits.any { it.nameWithExtension == "Stranger 2019.mp4" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun matches_folders_and_videos_together() = runTest {
        mediaRepository.videos.addAll(testVideos)
        mediaRepository.directories.add(
            Folder(
                name = "Avengers",
                path = "/root/Avengers",
                dateModified = 0L,
                mediaList = listOf(testVideos.first()),
            ),
        )

        useCase("avengers").test {
            val result = awaitItem()
            assertTrue(result.folders.any { it.name == "Avengers" })
            assertTrue(result.videos.any { it.nameWithExtension == "Avengers Endgame 2019.mp4" })
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun video(id: Long, name: String) = Video(
    id = id,
    duration = 1,
    uriString = "u$id",
    nameWithExtension = name,
    width = 1,
    height = 1,
    path = "/root/$name",
    size = 1,
)

private val testVideos = listOf(
    video(1, "Avengers Endgame 2019.mp4"),
    video(2, "Stranger 2019.mp4"),
    video(3, "Stranger Things S01.mp4"),
    video(4, "Unrelated clip.mp4"),
)
