package dev.vayou.tv.network

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.ChannelFavouritesStore
import dev.vayou.core.smb.IptvCountries
import dev.vayou.core.smb.IptvCountry
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.PlaylistStore
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.parseM3U
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The channels in whichever list is open, narrowed to the handful anybody wants.
 *
 * Fetched rather than stored: a channel list is somebody else's file and changes without asking,
 * and what is kept here is only its address. A thousand of them arrive at once, which is why the
 * narrowing -- the search, the group, the starred -- happens here and not in the grid: the screen
 * draws what came back, and the pass over the list runs when the question changes rather than on
 * every frame the focus moves.
 *
 * Which list is open is a question this asks itself. Opened from the home screen it is told; opened
 * from the bar it is not, and it takes the first one saved -- a viewer pressing "Channels" wants
 * channels, not a list of lists with one entry in it.
 */
@HiltViewModel
class TvChannelsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val favourites: ChannelFavouritesStore,
    private val playlists: PlaylistStore,
) : ViewModel() {

    /**
     * The file being read, or null until one is settled on.
     *
     * Not fixed even then: one of the seeded lists is iptv-org's, and choosing another country
     * there is choosing another file rather than filtering this one.
     */
    private val url = MutableStateFlow(savedStateHandle.get<String>(UrlArg)?.let(Uri::decode))

    /** Null until the fetch has answered, which is what tells an empty list from a slow one. */
    private val fetched = MutableStateFlow<Result<List<PlaylistChannel>>?>(null)

    private val query = MutableStateFlow("")
    private val group = MutableStateFlow<String?>(null)

    /**
     * Not held here, unlike the search and the group.
     *
     * Those two belong to the file they were typed against and go when the screen does. Looking at
     * the starred alone outlives it, as the chosen country does -- so it is read from the store,
     * and the first drawing of the screen is already the answer rather than a thousand channels
     * turning into twelve a frame later.
     */
    private val onlyStarred = favourites.isOnlyStarred

    /**
     * The file, filtered and blocked by letter -- the expensive half, and the half that only three
     * things change.
     *
     * Off the main thread, and kept apart from the starred set on purpose. A list runs to thousands
     * of channels; grouping and sorting that is tens of milliseconds, and doing it where the frames
     * are drawn is what a viewer feels as the screen sticking. Doing it again every time a star is
     * toggled would be the same cost for an answer that has not changed.
     */
    private val narrowed: Flow<Narrowed> = combine(fetched, query, group) { answer, query, group ->
        val channels = answer?.getOrNull()
        if (channels == null) {
            Narrowed(isLoading = answer == null, hasFailed = answer != null)
        } else {
            val kept = channels
                .filter { group == null || it.group == group }
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            Narrowed(
                isLoading = false,
                hasFailed = false,
                channels = kept,
                // Every group the file names, in the order it names them: an IPTV list is grouped
                // by whoever wrote it, and re-sorting it alphabetically loses the ordering they
                // meant -- the news together, the films together.
                groups = channels.mapNotNull { it.group?.takeIf(String::isNotBlank) }.distinct(),
                sections = kept.byLetter(),
            )
        }
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<TvChannelsState> = combine(
        narrowed,
        combine(query, group, onlyStarred, ::Triple),
        favourites.favourites,
        combine(url, playlists.playlists, ::Pair),
    ) { found, (query, group, onlyStarred), starredList, (address, saved) ->
        val starred = starredList.mapTo(HashSet(), PlaylistChannel::url)
        TvChannelsState(
            isLoading = found.isLoading,
            hasFailed = found.hasFailed,
            // Narrowed to the starred here rather than upstream, where the letters are worked out
            // for thousands of channels: that answer does not change when a star is toggled, and
            // re-deriving it every time somebody marked a channel would be the whole cost again for
            // a list the viewer can count.
            sections = if (onlyStarred) starredList.byLetter() else found.sections,
            groups = found.groups,
            group = group,
            query = query,
            starred = starred,
            // Read from the store rather than from what the narrowing left, which is the whole
            // point of them: a viewer who marked a channel wants it to hand whatever they are
            // looking at. Drawn from the filtered list, the block emptied itself the moment a
            // country or a group was chosen -- the marks were still there, and the shelf that was
            // meant to hold them was showing a slice of itself.
            //
            // Absent when the starred are all that is being shown: a block of them above a grid of
            // them would be the same list twice.
            favourites = if (onlyStarred) emptyList() else starredList,
            onlyStarred = onlyStarred,
            saved = saved,
            listName = saved.firstOrNull { it.url == address }?.name.orEmpty(),
            listUrl = address,
            country = address?.iptvCountry,
            isByCountry = address?.isIptvOrg == true,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(IdleTimeoutMs), TvChannelsState())

    init {
        // The first saved list, for a viewer who arrived by pressing "Channels" rather than by
        // opening one, and again whenever the open one is thrown away.
        viewModelScope.launch {
            playlists.playlists.collect { saved ->
                if (saved.none { it.url == url.value }) url.value = saved.firstOrNull()?.url
            }
        }
        viewModelScope.launch { url.filterNotNull().distinctUntilChanged().collect(::fetch) }
    }

    private suspend fun fetch(address: String) {
        fetched.value = null
        fetched.value = runCatching {
            withContext(Dispatchers.IO) { URL(address).openStream().bufferedReader().use { it.readText() } }
        }.map(::parseM3U)
    }

    fun selectPlaylist(playlist: SavedPlaylist) {
        clearNarrowing()
        url.value = playlist.url
    }

    /** Added and opened in one press: nobody adds a list in order to look at it later. */
    fun addPlaylist(name: String, address: String) {
        clearNarrowing()
        viewModelScope.launch {
            playlists.add(name, address)
            url.value = address
        }
    }

    /** The next list takes its place, chosen by the collector above. */
    fun removeCurrent() {
        val address = url.value ?: return
        viewModelScope.launch { playlists.remove(address) }
    }

    /**
     * Reads another country's list, and remembers it.
     *
     * Written to the store as well as read here, because the tile on the home screen is the same
     * list: a viewer who switched to Portugal and came back to a Brazilian tile would have been
     * given the choice and then had it taken away.
     */
    fun selectCountry(country: IptvCountry) {
        clearNarrowing()
        url.value = country.url
        viewModelScope.launch { playlists.setIptvCountry(country.code, LivePlaylistName) }
    }

    fun search(text: String) {
        query.value = text
    }

    /** Null is every group, which is what the first row of the chooser stands for. */
    fun selectGroup(name: String?) {
        group.value = name
        viewModelScope.launch { favourites.setOnlyStarred(false) }
    }

    /**
     * Only the channels the viewer marked.
     *
     * One of the answers the filter gives rather than a switch beside it: a switch is a press to
     * turn on and another to remember to turn off, and this is the same kind of question as "which
     * group" -- what am I looking at.
     */
    fun showOnlyStarred() {
        group.value = null
        viewModelScope.launch { favourites.setOnlyStarred(true) }
    }

    /**
     * The channel the viewer last opened, so coming back lands on it.
     *
     * Held here because the screen does not survive the player: navigating away takes its
     * composition apart, and a grid rebuilt from nothing puts the focus on whatever is first --
     * which, if that card has not been laid out yet, is the bar at the top of the screen.
     */
    var lastOpened: String? = null
        private set

    fun rememberOpened(channel: PlaylistChannel) {
        lastOpened = channel.url
    }

    fun toggleStar(channel: PlaylistChannel) {
        viewModelScope.launch { favourites.toggle(channel) }
    }

    /** A search and a group belong to the file they were typed against, not to the next one. */
    private fun clearNarrowing() {
        query.value = ""
        group.value = null
        viewModelScope.launch { favourites.setOnlyStarred(false) }
    }
}

/**
 * The letter a channel files under.
 *
 * Anything that is not a letter goes under one heading rather than one heading each: a list that
 * opens with "4", "5", "A&E", "[HD]" as four separate blocks is a list nobody can walk.
 */
private fun String.initial(): String {
    val first = firstOrNull { it.isLetterOrDigit() } ?: return OtherLetter
    return if (first.isLetter()) first.uppercaseChar().toString() else OtherLetter
}

/** Seeded by the app rather than added by the viewer, and so swappable for another country's. */
private val String.isIptvOrg: Boolean
    get() = this == IptvCountry.GlobalUrl || startsWith(IptvCountry.CountryPrefix)

private val String.iptvCountry: IptvCountry?
    get() {
        if (!isIptvOrg) return null
        val code = removePrefix(IptvCountry.CountryPrefix).removeSuffix(".m3u").takeIf { it.length == CodeLength }
        return IptvCountries.firstOrNull { it.code == code }
    }

/** What the seeded list is called, so switching country does not rename the tile it came from. */
private const val LivePlaylistName = "Canais ao vivo"

private const val CodeLength = 2

/** The expensive half of the answer, worked out once per question rather than once per star. */
private data class Narrowed(
    val isLoading: Boolean,
    val hasFailed: Boolean,
    val channels: List<PlaylistChannel> = emptyList(),
    val groups: List<String> = emptyList(),
    val sections: List<TvChannelSection> = emptyList(),
)

data class TvChannelSection(val letter: String, val channels: List<PlaylistChannel>)

data class TvChannelsState(
    val isLoading: Boolean = true,
    val hasFailed: Boolean = false,
    val sections: List<TvChannelSection> = emptyList(),
    val groups: List<String> = emptyList(),
    val group: String? = null,
    val onlyStarred: Boolean = false,
    val query: String = "",
    val starred: Set<String> = emptySet(),
    /** The starred ones of whatever is being shown, drawn before the first letter. */
    val favourites: List<PlaylistChannel> = emptyList(),
    /** Every list this television knows, for the chooser in the header. */
    val saved: List<SavedPlaylist> = emptyList(),
    val listName: String = "",
    val listUrl: String? = null,
    /** The country whose list this is, for the one list that has countries. */
    val country: IptvCountry? = null,
    val isByCountry: Boolean = false,
) {
    /** Whether the file has arrived and had something in it, which is when narrowing means anything. */
    val hasList: Boolean = !isLoading && !hasFailed && (groups.isNotEmpty() || sections.isNotEmpty())

    /** The seeded list is not the viewer's to throw away; it is swapped for another country instead. */
    val canRemove: Boolean = listUrl != null && !isByCountry
}

const val UrlArg = "url"

/** Numbers and symbols, under one heading at the end. */
const val OtherLetter = "#"

private const val IdleTimeoutMs = 5_000L

/**
 * Channels blocked by the letter they start with, in the order a viewer reads them.
 *
 * Letters in their order, and the block of numbers and symbols after them rather than before: it is
 * the leftovers, and leftovers go at the end.
 */
private fun List<PlaylistChannel>.byLetter(): List<TvChannelSection> = groupBy { it.name.initial() }
    .map { (letter, found) -> TvChannelSection(letter, found) }
    .sortedWith(compareBy({ it.letter == OtherLetter }, { it.letter }))
