package dev.vayou.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.vayou.core.model.Video
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.NetworkServerEntry
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.graphics.rememberArtworkTint

/**
 * The first thing on the television.
 *
 * Rows rather than a grid or a tree: a D-pad walks a row, and what a viewer wants is nearly always
 * the first card of one of them. Everything a row only shows the head of is in the library, one
 * press away along the bar above.
 */
@Composable
fun TvHomeScreen(
    onPlayVideo: (Video) -> Unit,
    /** A film reached by address alone -- what a share hands back, having no library entry. */
    onPlayNetwork: (String) -> Unit,
    onOpenServer: (String) -> Unit,
    onOpenFolder: (FavoriteFolder) -> Unit,
    onOpenPlaylist: (SavedPlaylist) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit,
    viewModel: TvHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isAddingServer by remember { mutableStateOf(false) }
    var isAddingPlaylist by remember { mutableStateOf(false) }

    /** The card whose options are up, or null while the rows are just rows. */
    var acting by remember { mutableStateOf<HomeAction?>(null) }

    /**
     * The card the focus lands on, and which row owns it.
     *
     * Named rather than left to Compose, because what Compose picks on the first frame is the
     * navigation: a lazy column has composed nothing yet, and the rail is the only thing on screen
     * willing to take it -- which opened the rail over the screen the viewer had just arrived at.
     *
     * Whichever row is on top, since every row above the servers is there only when it has
     * something in it.
     */
    val landing = remember { FocusRequester() }
    val landingRow = when {
        state.recent.isNotEmpty() -> HomeRow.Recent
        state.videos.isNotEmpty() -> HomeRow.Videos
        state.folders.isNotEmpty() -> HomeRow.Folders
        else -> HomeRow.Servers
    }
    LaunchedEffect(landingRow, state.servers) {
        withFrameNanos { }
        runCatching { landing.requestFocus() }
    }

    // Boxed, so the dialog below has something to be laid over. As siblings in a column, the rows
    // took the whole height and the dialog was measured into what was left, which was nothing: the
    // card opened, the state flipped, and no dialog was ever drawn.
    // What the card under the focus looks like, or null where it is a mark rather than a picture.
    // Held here and not in each row, because there is one background and it answers to whichever
    // card has the focus, wherever that card is.
    var focused: Any? by remember { mutableStateOf(null) }

    // The colour is read from a 24-pixel copy of a picture that is already on screen, so it is a
    // cache hit and a scan of a few hundred pixels off the main thread. Surface where there is
    // nothing to read: a row of folders and servers lets the screen settle rather than inventing
    // a colour for a glyph.
    val surface = MaterialTheme.colorScheme.surface
    val tint = rememberArtworkTint(model = focused, fallback = surface)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Down and not across, unlike the sleeve: this screen is rows stacked downwards, and a
            // wash that follows them is one the eye reads as depth rather than as a second element.
            // Gone by two thirds, so the lower rows keep the contrast the titles were chosen for.
            .background(
                Brush.verticalGradient(
                    0f to tint,
                    BackdropMidpoint to lerp(tint, surface, BackdropMidBlend),
                    1f to surface,
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The two marks the phone keeps in its header, here for the same reason: they are things
            // to do rather than places to go. A place belongs in a row with the others; a thing to do
            // belongs above them, out of the way of the walk.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TvScreenInset, vertical = TvRowInset),
                horizontalArrangement = Arrangement.spacedBy(TvRowGap, Alignment.End),
            ) {
                TvIconButton(VayouIcons.Search, stringResource(R.string.search), onSearch)
                TvIconButton(VayouIcons.Settings, stringResource(R.string.settings), onOpenSettings)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = TvScreenInset),
                verticalArrangement = Arrangement.spacedBy(RowGap),
            ) {
                if (state.recent.isNotEmpty()) {
                    item {
                        CardRow(
                            title = stringResource(R.string.continue_watching),
                            items = state.recent,
                            firstCard = landing.takeIf { landingRow == HomeRow.Recent },
                            key = TvRecent::id,
                        ) { entry, cardModifier ->
                            when (entry) {
                                is TvRecent.Local -> Card(
                                    entry.video.displayName,
                                    { onPlayVideo(entry.video) },
                                    cardModifier.reporting(entry.video.uriString) { focused = it },
                                ) {
                                    AsyncImage(
                                        model = entry.video.uriString,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    WatchedBar(entry.watched)
                                }
                                // A mark and not a frame. Taking a frame out of a file on a share
                                // means opening it over the network, and doing that for a row that
                                // is only being drawn would hold the screen on a server that may be
                                // switched off. The card says where the film lives; the picture
                                // waits until somebody asks for the film itself.
                                is TvRecent.Remote -> Card(
                                    entry.displayName,
                                    { onPlayNetwork(entry.uri) },
                                    cardModifier.reporting(null) { focused = it },
                                ) {
                                    TvCardMark(VayouIcons.Video)
                                    WatchedBar(entry.watched)
                                }
                            }
                        }
                    }
                }

                if (state.videos.isNotEmpty()) {
                    item {
                        CardRow(
                            title = stringResource(R.string.videos),
                            items = state.videos,
                            firstCard = landing.takeIf { landingRow == HomeRow.Videos },
                            key = Video::uriString,
                        ) { video, cardModifier ->
                            Card(
                                video.displayName,
                                { onPlayVideo(video) },
                                cardModifier.reporting(video.uriString) { focused = it },
                            ) {
                                AsyncImage(
                                    model = video.uriString,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }

                // Above the machines they are on, because a viewer who pinned one is saying that is where
                // they go: the row of servers below is for the evenings they want something else.
                if (state.folders.isNotEmpty()) {
                    item {
                        CardRow(
                            title = stringResource(R.string.pinned_folders),
                            items = state.folders,
                            firstCard = landing.takeIf { landingRow == HomeRow.Folders },
                            // Interpolated, not escaped. Written with the dollar quoted, every folder answered with the
                            // same literal text for a key, and a lazy row refuses two items that claim to be
                            // the same one -- so pinning a second folder crashed the screen on open.
                            key = { "${it.host}/${it.share}/${it.path}" },
                        ) { folder, cardModifier ->
                            Tile(
                                title = folder.displayName,
                                onClick = { onOpenFolder(folder) },
                                modifier = cardModifier.reporting(null) { focused = it },
                                onLongClick = { acting = HomeAction.Unpin(folder) },
                            ) { TvCardFolder() }
                        }
                    }
                }

                // Always, even with nothing in it, because the card on the end is the way in. A television
                // whose network discovery finds nothing and whose phone has saved nothing had no way to
                // reach a machine at all: the row was hidden precisely when it was needed.
                item {
                    CardRow(
                        title = stringResource(R.string.servers),
                        items = state.servers,
                        firstCard = landing.takeIf { landingRow == HomeRow.Servers },
                        key = NetworkServerEntry::host,
                        trailing = {
                            Tile(
                                title = stringResource(R.string.add_server),
                                onClick = { isAddingServer = true },
                            ) { TvCardMark(VayouIcons.Add) }
                        },
                    ) { server, cardModifier ->
                        Tile(
                            title = server.displayName,
                            onClick = { onOpenServer(server.host) },
                            modifier = cardModifier.reporting(null) { focused = it },
                            // Only for the saved: a machine merely found on the wire has nothing to
                            // forget, and would be back the moment discovery saw it again.
                            onLongClick = {
                                acting = HomeAction.Forget(server.host, server.displayName)
                            }.takeIf { server.isSaved },
                        ) {
                            // Saved is a server; found on the wire and not saved is still just a
                            // machine answering, which is what the phone says with the same two marks.
                            TvCardMark(if (server.isSaved) VayouIcons.Network else VayouIcons.Wifi)
                        }
                    }
                }

                // The places, last and as cards, which is the whole of what the navigation used to be.
                //
                // A bar is a second thing to learn and a press to reach before anything can be chosen; four
                // cards at the foot of the rows a viewer is already walking cost nothing to find and nothing
                // to explain. What is above them is what this television has; this row is where the rest is.
                item {
                    CardRow(
                        title = stringResource(R.string.more),
                        items = Destinations,
                        key = Destination::label,
                    ) { destination, cardModifier ->
                        Tile(
                            title = stringResource(destination.label),
                            onClick = {
                                when (destination) {
                                    Destination.Library -> onOpenLibrary()
                                    Destination.Music -> onOpenMusic()
                                }
                            },
                            modifier = cardModifier.reporting(null) { focused = it },
                        ) { TvCardMark(destination.icon) }
                    }
                }

                // Always, and with the way to add one on the end, exactly as the servers row has
                // it. Hidden when empty, the row went missing precisely when a viewer had no list
                // and needed to make one -- and the answer to that used to be a second Channels card
                // further down, which put the same place on this screen twice.
                item {
                    CardRow(
                        title = stringResource(R.string.channels),
                        items = state.playlists,
                        key = SavedPlaylist::url,
                        trailing = {
                            Tile(
                                title = stringResource(R.string.add_playlist),
                                onClick = { isAddingPlaylist = true },
                            ) { TvCardMark(VayouIcons.Add) }
                        },
                    ) { playlist, cardModifier ->
                        Tile(
                            playlist.name,
                            { onOpenPlaylist(playlist) },
                            cardModifier.reporting(null) { focused = it },
                        ) {
                            TvCardMark(VayouIcons.Tv)
                        }
                    }
                }
            }
        }

        acting?.let { action ->
            val option = when (action) {
                is HomeAction.Forget -> TvOptionItem(VayouIcons.Delete, stringResource(R.string.forget_server)) {
                    viewModel.forgetServer(action.host)
                }

                is HomeAction.Unpin -> TvOptionItem(
                    VayouIcons.Pin,
                    stringResource(R.string.unpin_folder),
                ) { viewModel.unpinFolder(action.folder) }

                is HomeAction.RemoveList -> TvOptionItem(
                    VayouIcons.Delete,
                    stringResource(R.string.remove_playlist),
                ) { viewModel.removePlaylist(action.playlist.url) }
            }
            TvOptions(
                title = action.name,
                options = listOf(option),
                onDismiss = { acting = null },
                face = { TvCardMark(action.mark) },
            )
        }

        if (isAddingPlaylist) {
            TvAddPlaylist(
                onAdd = { name, url ->
                    isAddingPlaylist = false
                    viewModel.addPlaylist(name, url)
                },
                onDismiss = { isAddingPlaylist = false },
            )
        }

        if (isAddingServer) {
            AddServer(
                onDismiss = { isAddingServer = false },
                onAdd = { host ->
                    isAddingServer = false
                    viewModel.rememberServer(host)
                    onOpenServer(host)
                },
            )
        }
    }
}

/** A titled row of cards, scrolled by the D-pad rather than by a thumb. */
@Composable
private fun <T> CardRow(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    /**
     * Handed to the first card of the first row on the screen, and null for every row below it.
     *
     * Without it the focus went to the rail, which then opened over the very screen the viewer had
     * just arrived at: a lazy column has composed nothing on the first frame, so the only thing
     * willing to take the focus was the navigation.
     */
    firstCard: FocusRequester? = null,
    /** A card after the last of them, for a row that offers something as well as listing things. */
    trailing: (@Composable () -> Unit)? = null,
    card: @Composable (T, Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TvCardTitleGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = TvScreenInset),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = TvScreenInset),
            horizontalArrangement = Arrangement.spacedBy(TvCardGap),
        ) {
            itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
                card(item, if (index == 0 && firstCard != null) Modifier.focusRequester(firstCard) else Modifier)
            }
            trailing?.let { item { it() } }
        }
    }
}

/**
 * An address, typed once and kept.
 *
 * The only thing asked for is where the machine is. What it is called and what it wants for a
 * password are answered by the machine itself, or by the screen that opens next -- asking for three
 * things on a D-pad when two of them can be found out is three minutes of somebody's evening.
 */
@Composable
private fun AddServer(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var address by rememberSaveable { mutableStateOf("") }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    TvDialog(title = stringResource(R.string.add_server), onDismiss = onDismiss) {
        TvTextField(
            value = address,
            onValueChange = { address = it },
            label = stringResource(R.string.server_address),
            modifier = Modifier.focusRequester(first),
        )
        Spacer(modifier = Modifier.height(TvRowGap))
        TvChoiceRow(label = stringResource(R.string.cancel), isSelected = false, onClick = onDismiss)
        // Absent until there is an address rather than greyed out: a D-pad walks past a disabled row
        // as if it were not there, so a dim one is a gap with a word in it.
        if (address.isNotBlank()) {
            TvChoiceRow(label = stringResource(R.string.connect), isSelected = false) { onAdd(address.trim()) }
        }
    }
}

/**
 * A place to go, at the one width a row has to state for itself.
 *
 * A row has nothing to measure a card against, which is why the width is here and nowhere else.
 */
@Composable
private fun Tile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    mark: @Composable BoxScope.() -> Unit,
) {
    TvTile(
        title = title,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(TvCardWidth),
        mark = mark,
    )
}

/** A thing to play, with its own picture on it. */
@Composable
private fun Card(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: @Composable BoxScope.() -> Unit,
) {
    TvCard(title = title, onClick = onClick, modifier = modifier.width(TvCardWidth), face = face)
}

/**
 * How far into a thing the viewer got, along the bottom edge of its card.
 *
 * Drawn on the card and not under it, where a row of them would push the titles down and change
 * the height of every card in the row for the sake of the few that have been started.
 *
 * Nothing at all where the fraction is not known -- a film watched before lengths were written
 * down has a position and no total, and a bar guessed from that would be a bar that lies.
 */
@Composable
private fun BoxScope.WatchedBar(watched: Float?) {
    if (watched == null || watched <= 0f) return
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(BarHeight)
            // Translucent white under the fill, as the phone draws it: this lies on a frame nobody
            // chose, and a colour from the palette reads as a block on some of them.
            .background(Color.White.copy(alpha = BarTrackAlpha)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(watched)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private val BarHeight = 4.dp

private const val BarTrackAlpha = 0.4f

/**
 * Says what this card looks like when the focus arrives on it, and nothing when it leaves.
 *
 * Only on arrival: a focus move is one card losing it and another taking it, and clearing on the
 * way out would blank the background for the frame in between, which reads as a flicker.
 */
private fun Modifier.reporting(artwork: Any?, onFocused: (Any?) -> Unit): Modifier =
    onFocusChanged { if (it.isFocused) onFocused(artwork) }

/** Where the wash has given way, and how far it has gone by then. */
private const val BackdropMidpoint = 0.45f

private const val BackdropMidBlend = 0.7f

private val RowGap = 32.dp

/** Which row is on top, and so which card the focus lands on when the screen opens. */
private enum class HomeRow { Recent, Videos, Folders, Servers }

/** Where the rest of the app is, now that there is no bar to name it. */
private enum class Destination(val label: Int, val icon: ImageVector) {
    Library(R.string.videos, VayouIcons.VideoLibrary),
    Music(R.string.music, VayouIcons.Audio),
}

private val Destinations = Destination.entries

/** What is being acted on, and so which options the menu shows. */
private sealed interface HomeAction {
    /** What the menu is about, at the head of it. */
    val name: String

    /** The card's own mark, so the menu shows the thing that was held rather than a blank plate. */
    val mark: ImageVector

    class Forget(val host: String, override val name: String) : HomeAction {
        override val mark = VayouIcons.Network
    }

    class Unpin(val folder: FavoriteFolder) : HomeAction {
        override val name = folder.displayName
        override val mark = VayouIcons.Folder
    }

    class RemoveList(val playlist: SavedPlaylist) : HomeAction {
        override val name = playlist.name
        override val mark = VayouIcons.Tv
    }
}
