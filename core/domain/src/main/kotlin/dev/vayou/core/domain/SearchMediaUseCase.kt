package dev.vayou.core.domain

import dev.vayou.core.common.Dispatcher
import dev.vayou.core.common.VayouDispatchers
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

/** What the library found, best first. */
data class SearchResults(val folders: List<Folder> = emptyList(), val videos: List<Video> = emptyList()) {
    val isEmpty: Boolean get() = folders.isEmpty() && videos.isEmpty()
}

/**
 * Finds films and folders by name, ordered by how well each answers what was typed.
 *
 * Scored rather than filtered. "Contains the word" puts a file whose name merely mentions the query
 * above the one actually called it, and on a library of a thousand files that is the difference
 * between finding a film and scrolling for it.
 *
 * The whole library is searched in memory rather than by the database. It is already collected for
 * the list on screen, it is a few thousand rows at most, and a query re-run per keystroke against
 * SQLite would be a round trip per letter.
 */
class SearchMediaUseCase @Inject constructor(
    private val getSortedVideos: GetSortedVideosUseCase,
    private val getSortedFolders: GetSortedFoldersUseCase,
    @Dispatcher(VayouDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {

    operator fun invoke(query: String): Flow<SearchResults> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return flowOf(SearchResults())

        return combine(getSortedVideos(), getSortedFolders()) { videos, folders ->
            val matcher = SearchMatcher(trimmed)
            SearchResults(
                folders = folders.scoredBy { matcher.scoreOf(it.name, it.path) },
                videos = videos.scoredBy { matcher.scoreOf(it.nameWithExtension, it.path) },
            )
        }.flowOn(defaultDispatcher)
    }
}

/** Drops what does not match at all, and puts the best of the rest first. */
private inline fun <T> List<T>.scoredBy(crossinline score: (T) -> Int): List<T> = asSequence()
    .map { it to score(it) }
    .filter { it.second > 0 }
    .sortedByDescending { it.second }
    .map { it.first }
    .toList()

/**
 * How well a name answers a query, as a number. Zero is "not at all".
 *
 * Built once per query rather than per file: the pattern below is compiled, and a library of a
 * thousand files would otherwise compile it a thousand times for one keystroke.
 */
private class SearchMatcher(query: String) {

    private val phrase = query.lowercase()

    private val words = phrase.split(WhitespaceRun).filter { it.isNotBlank() }

    /** Only for a query of several words; one word is already covered by the phrase test. */
    private val wordsInOrder: Regex? = if (words.size > 1) {
        Regex(words.joinToString(".*") { Regex.escape(it) }, RegexOption.IGNORE_CASE)
    } else {
        null
    }

    /** The best any of [texts] manages -- a film found by its folder counts as found. */
    fun scoreOf(vararg texts: String): Int = texts.maxOf { scoreOfText(it.lowercase()) }

    private fun scoreOfText(text: String): Int {
        // The whole phrase, which beats everything else. A file called what was typed is the answer.
        if (text.contains(phrase)) {
            val atWordStart = if (startsAWordIn(text, phrase)) WordStartBonus else 0
            val atNameStart = if (text.startsWith(phrase)) NameStartBonus else 0
            return PhraseScore + atWordStart + atNameStart
        }
        if (words.size <= 1) return NoMatch

        // Every word, in the order typed, gaps allowed: "stranger 2019" finds "Stranger Things 2019".
        if (wordsInOrder?.containsMatchIn(text) == true) return InOrderScore

        // Every word, any order. Worth less, and worth more the more of them start a word.
        if (words.all(text::contains)) {
            return AnyOrderScore + words.count { startsAWordIn(text, it) } * PerWordBonus
        }
        return NoMatch
    }

    /** True where the match begins a word rather than landing inside one. */
    private fun startsAWordIn(text: String, part: String): Boolean {
        val index = text.indexOf(part)
        return index == 0 || (index > 0 && text[index - 1] in WordBoundaries)
    }
}

private val WhitespaceRun = Regex("\\s+")

/** What parts a name into words in a filename, which is not the same set as in prose. */
private val WordBoundaries = setOf(' ', '_', '-', '.', '/', '\\', '[', ']', '(', ')')

private const val NoMatch = 0

private const val AnyOrderScore = 200

private const val PerWordBonus = 20

private const val InOrderScore = 500

private const val PhraseScore = 1_000

private const val WordStartBonus = 50

private const val NameStartBonus = 30
