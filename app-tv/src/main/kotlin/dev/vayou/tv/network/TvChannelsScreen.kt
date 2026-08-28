package dev.vayou.tv.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.vayou.core.smb.IptvCountries
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.tv.R
import dev.vayou.tv.TvAction
import dev.vayou.tv.TvActions
import dev.vayou.tv.TvAddPlaylist
import dev.vayou.tv.TvBackButton
import dev.vayou.tv.TvCard
import dev.vayou.tv.TvCardGap
import dev.vayou.tv.TvCardMark
import dev.vayou.tv.TvCardWidth
import dev.vayou.tv.TvChoiceList
import dev.vayou.tv.TvChoiceRow
import dev.vayou.tv.TvDialog
import dev.vayou.tv.TvIconButton
import dev.vayou.tv.TvMessage
import dev.vayou.tv.TvOptionItem
import dev.vayou.tv.TvOptions
import dev.vayou.tv.TvRowGap
import dev.vayou.tv.TvRowInset
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvTextField
import dev.vayou.tv.TvTitleInset

/**
 * The channels in one list.
 *
 * A grid and not a row, unlike the home screen: a channel list runs to hundreds, and a single row
 * that long is a viewer holding right for a minute. Down a grid, the same reach covers five at a
 * time, under a heading for each letter -- from three metres the names all read alike, and the
 * letter is what says how far down the viewer has got.
 */
@Composable
fun TvChannelsScreen(
    onPlay: (PlaylistChannel) -> Unit,
    onBack: () -> Unit,
    viewModel: TvChannelsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isSearching by remember { mutableStateOf(false) }
    var isFiltering by remember { mutableStateOf(false) }
    var isChoosingList by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }

    /** The channel whose options are up, or null while the grid is just a grid. */
    var acting by remember { mutableStateOf<PlaylistChannel?>(null) }

    // The channel the viewer came back from, or the first one. Left to itself Compose gives the
    // focus to whatever can take it first, which is the bar at the top -- and being dropped there
    // after every channel means finding your place again each time.
    val landing = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val landingUrl = viewModel.lastOpened
    val landingIndex = remember(state.sections, state.starred, landingUrl) {
        state.indexOf(landingUrl)
    }
    LaunchedEffect(state.sections, isSearching) {
        if (state.sections.isEmpty() || isSearching) return@LaunchedEffect
        // Scrolled to first: a lazy grid has not composed a card four hundred rows down, and a
        // requester with nothing attached to it is a focus that goes nowhere.
        landingIndex?.let { grid.scrollToItem(it) }
        withFrameNanos { }
        runCatching { landing.requestFocus() }
    }

    // Search closes before the screen does: it took a press to open and it takes one to shut.
    BackHandler(enabled = isSearching) {
        viewModel.search("")
        isSearching = false
    }

    // Opaque, because the screen it is pushed over is still drawn underneath while the
    // two are being swapped: through a transparent one, the cards of the screen behind
    // show as a shadow across this one.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                state = state,
                onBack = onBack,
                isSearching = isSearching,
                onSearch = viewModel::search,
                onOpenSearch = { isSearching = true },
                onOpenFilter = { isFiltering = true },
                onOpenLists = { isChoosingList = true },
            )

            when {
                // Only where a list is what is being shown. The starred are drawn from disc and owe
                // nothing to a saved list -- a viewer who threw the last one away would otherwise be
                // told they have no lists while looking at the very channels they starred.
                !state.onlyStarred && state.listUrl == null ->
                    TvMessage(stringResource(R.string.no_channel_lists))
                state.isLoading -> TvMessage(stringResource(R.string.loading_channels))
                state.hasFailed -> TvMessage(stringResource(R.string.channels_unreachable))
                // The two ways of having nothing are not the same sentence. A list can be empty
                // because the file is; the starred are empty because nobody has starred anything,
                // and that one has an answer the viewer can act on.
                state.sections.isEmpty() && state.onlyStarred ->
                    TvMessage(stringResource(R.string.no_favourite_channels))
                state.sections.isEmpty() -> TvMessage(stringResource(R.string.no_channels))
                else -> ChannelGrid(
                    state = state,
                    grid = grid,
                    landing = landing,
                    landingUrl = landingUrl,
                    onPlay = { channel ->
                        viewModel.rememberOpened(channel)
                        onPlay(channel)
                    },
                    onOptions = { acting = it },
                )
            }
        }

        // Every list this television knows, and the two things that can be done to the set of them.
        // Here rather than on a screen of its own: choosing between lists is the only moment anyone
        // wants to add or drop one, and most viewers have exactly one and never open this at all.
        if (isChoosingList) {
            TvDialog(title = stringResource(R.string.channel_lists), onDismiss = { isChoosingList = false }) {
                state.saved.forEach { playlist ->
                    TvChoiceRow(label = playlist.name, isSelected = playlist.url == state.listUrl) {
                        viewModel.selectPlaylist(playlist)
                        isChoosingList = false
                    }
                }
                Spacer(modifier = Modifier.height(TvRowGap))
                // Not two more rows of the list above: those are the lists to choose between, and
                // these are what can be done to them. Drawn as rows they took the same tick gutter
                // and left it empty, so the words sat indented under a column of marks.
                TvActions {
                    TvAction(stringResource(R.string.add_playlist)) {
                        isChoosingList = false
                        isAdding = true
                    }
                    if (state.canRemove) {
                        TvAction(stringResource(R.string.remove_playlist)) {
                            viewModel.removeCurrent()
                            isChoosingList = false
                        }
                    }
                }
            }
        }

        acting?.let { channel ->
            val isStarred = channel.url in state.starred
            TvOptions(
                face = { Logo(channel) },
                title = channel.name,
                subtitle = channel.group,
                onDismiss = { acting = null },
                options = listOf(
                    TvOptionItem(
                        // As on the card, which marks a favourite with a filled star.
                        icon = if (isStarred) VayouIcons.StarFilled else VayouIcons.StarOutlined,
                        label = stringResource(
                            if (isStarred) R.string.remove_favourite else R.string.add_favourite,
                        ),
                    ) { viewModel.toggleStar(channel) },
                ),
            )
        }

        if (isAdding) {
            TvAddPlaylist(
                onAdd = { name, address ->
                    viewModel.addPlaylist(name, address)
                    isAdding = false
                },
                onDismiss = { isAdding = false },
            )
        }

        if (isFiltering) {
            // Two different questions wearing one button. On the list the app seeds, the countries are
            // separate files and choosing one is choosing another file; on anybody else's list, the
            // groups are inside the file that is already open.
            // The starred sit at the head of both lists, because "only the ones I marked" is the
            // one narrowing that means the same thing whatever the list underneath is.
            // No answer here about the starred any more: they are a screen of their own, reached
            // from the home, and offering them as one of a file's groups is what tied the two
            // together -- a viewer who looked at their favourites found the next list they opened
            // still filtered to them.
            if (state.isByCountry) {
                TvChoiceList(
                    title = stringResource(R.string.country),
                    options = IptvCountries.map { it to it.name },
                    selected = state.country,
                    // Never null in practice: the list only holds countries. Typed to match the
                    // selected one, which is null for a list that is not held by country at all.
                    onChoose = { country -> country?.let(viewModel::selectCountry) },
                    onDismiss = { isFiltering = false },
                )
            } else {
                TvChoiceList(
                    title = stringResource(R.string.group),
                    options = listOf(AllGroups to stringResource(R.string.group_all)) +
                        state.groups.map { it to it },
                    selected = state.group ?: AllGroups,
                    onChoose = { choice -> viewModel.selectGroup(choice.takeIf { it != AllGroups }) },
                    onDismiss = { isFiltering = false },
                )
            }
        }
    }
}

/**
 * The channels, starred first and then by letter.
 *
 * Its own composable rather than a block inside the screen: the grid's content is not a composable
 * scope, so every name it draws has to be read out here where the resources still are.
 */
@Composable
private fun ChannelGrid(
    state: TvChannelsState,
    grid: LazyGridState,
    landing: FocusRequester,
    /** The channel to come back to, or null to come back to the first one. */
    landingUrl: String?,
    onPlay: (PlaylistChannel) -> Unit,
    onOptions: (PlaylistChannel) -> Unit,
) {
    LazyVerticalGrid(
        state = grid,
        columns = GridCells.Adaptive(TvCardWidth),
        contentPadding = PaddingValues(horizontal = TvScreenInset, vertical = TvTitleInset),
        horizontalArrangement = Arrangement.spacedBy(TvCardGap),
        verticalArrangement = Arrangement.spacedBy(TvCardGap),
    ) {
        state.sections.forEachIndexed { sectionIndex, section ->
            heading(section.letter)
            itemsIndexed(section.channels, key = { _, channel -> channel.url }) { index, channel ->
                ChannelCard(
                    channel = channel,
                    isStarred = channel.url in state.starred,
                    onPlay = { onPlay(channel) },
                    onOptions = { onOptions(channel) },
                    modifier = if (
                        channel.url.isLanding(
                            landingUrl,
                            isFirst = sectionIndex == 0 && index == 0,
                        )
                    ) {
                        Modifier.focusRequester(landing)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

/** The card the focus lands on: the one left behind, or the first where there is none to go back to. */
private fun String.isLanding(landingUrl: String?, isFirst: Boolean): Boolean =
    if (landingUrl == null) isFirst else this == landingUrl

/**
 * Where in the grid a channel sits, counting the headings, or null for one that is not shown.
 *
 * The grid numbers every item it draws and a heading is one of them, so a count that skipped them
 * would scroll to a card some rows short of the one asked for.
 */
private fun TvChannelsState.indexOf(url: String?): Int? {
    if (url == null) return null
    var index = 0
    sections.forEach { section ->
        index++
        section.channels.forEachIndexed { position, channel -> if (channel.url == url) return index + position }
        index += section.channels.size
    }
    return null
}

/** A line across the grid, naming the block under it. */
private fun LazyGridScope.heading(text: String) {
    item(key = "head:$text", span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = TvRowInset),
        )
    }
}

/**
 * The name, what the list is narrowed to, and the way in to narrowing it further.
 *
 * Search is an icon until it is pressed, and then it takes the name's place. A field standing open
 * on arrival is a field that has the focus, and a field that has the focus on a television is an
 * on-screen keyboard over the thing the viewer came to look at.
 */
@Composable
private fun Header(
    state: TvChannelsState,
    onBack: () -> Unit,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenLists: () -> Unit,
) {
    val field = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) runCatching { field.requestFocus() } }

    Row(
        modifier = Modifier.padding(horizontal = TvScreenInset, vertical = TvRowInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvTitleInset),
    ) {
        if (isSearching) {
            Box(modifier = Modifier.weight(1f)) {
                TvTextField(
                    value = state.query,
                    onValueChange = onSearch,
                    label = stringResource(R.string.search),
                    modifier = Modifier.focusRequester(field),
                )
            }
            return@Row
        }

        TvBackButton(label = stringResource(R.string.back), onBack = onBack)
        // The one place that says what is on the screen. It used to say only which list was
        // open, and the filter beside it repeated the answer for a list held by country -- the same
        // word twice across one header -- while saying nothing the title did not for the others.
        Text(
            text = when {
                state.onlyStarred -> stringResource(R.string.favourites)
                state.isByCountry -> state.country?.name ?: state.listName
                else -> state.group ?: state.listName
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!state.hasList) return@Row

        // Neither of these belongs to the starred: one names the file being read and the other
        // narrows it, and the starred are not one file. What is left is the search, which does
        // narrow them.
        if (state.onlyStarred) {
            TvIconButton(icon = VayouIcons.Search, label = stringResource(R.string.search), onClick = onOpenSearch)
            return@Row
        }

        // Only where there is a choice to make. One list and this pill says what the title already
        // says, and costs a press to reach on the way to the ones that do something.
        if (state.saved.size > 1) {
            Pill(
                label = state.listName,
                icon = VayouIcons.Tv,
                isSelected = false,
                onClick = onOpenLists,
            )
        }
        // A mark and no word. What the filter is set to is worth knowing, which is why the
        // title now carries it; carried here as well it was the same answer written twice, three
        // inches apart, in a header whose job is to stay out of the way.
        TvIconButton(icon = VayouIcons.Filter, label = stringResource(R.string.group), onClick = onOpenFilter)
        // A mark rather than a pill, as the back and the gear are: the pills beside it each carry a
        // word and stand for a choice the viewer has made, and this one is a thing to do.
        TvIconButton(icon = VayouIcons.Search, label = stringResource(R.string.search), onClick = onOpenSearch)
    }
}

/**
 * One thing the header can say or do, in a rounded plate.
 *
 * Amber while it is on and white under the focus, which are two different questions: what the list
 * is narrowed to, and where the remote is. One colour for both would leave a viewer unable to tell
 * a filter they set from the one they are about to.
 */
@Composable
private fun Pill(label: String?, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.extraLarge),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            focusedContainerColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TvRowInset, vertical = TvRowGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvRowGap),
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(PillIcon))
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = PillLabelWidth),
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: PlaylistChannel,
    isStarred: Boolean,
    onPlay: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvCard(
        title = channel.name,
        subtitle = channel.group,
        onClick = onPlay,
        // Held rather than tapped: starring is the second thing anyone does to a channel, and it
        // does not deserve a button on every card in a list of hundreds.
        onLongClick = onOptions,
        modifier = modifier,
    ) {
        Logo(channel)
        if (isStarred) {
            Icon(
                imageVector = VayouIcons.StarFilled,
                contentDescription = stringResource(R.string.starred),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(StarInset)
                    .size(StarSize),
            )
        }
    }
}

private val LogoInset = 16.dp

private val StarInset = 8.dp

private val StarSize = 20.dp

private val PillIcon = 18.dp

/** A group's name can run to a sentence; the pill says what it can and stops. */
private val PillLabelWidth = 220.dp

/** The one answer in the group chooser that is not a group. */
private const val AllGroups = ""

/** What a station is known by, on its card and in the menu of what can be done to it. */
@Composable
private fun BoxScope.Logo(channel: PlaylistChannel) {
    if (channel.logo == null) {
        // Neutral: a channel is a thing that plays. The amber is for what opens into something,
        // which on this screen is nothing.
        TvCardMark(VayouIcons.Tv)
        return
    }
    AsyncImage(
        model = channel.logo,
        contentDescription = null,
        // Fitted, not filled: a station's mark cropped to a rectangle is a station nobody
        // recognises.
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .padding(LogoInset),
    )
}
