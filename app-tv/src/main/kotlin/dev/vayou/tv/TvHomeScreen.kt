package dev.vayou.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    /** The starred channels of every list, which is a place of its own and not part of one. */
    onOpenStarredChannels: () -> Unit,
    onOpenSettings: () -> Unit,
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

    // The card the viewer left from, if its row is still on the screen. Coming back to the top of
    // the first row meant coming back to a different card each time: play something and it is at
    // the head of the row of things to continue, so the card just left is the one the focus is
    // taken away from.
    val opened = viewModel.lastOpened
    // In the order a television is opened for: what was being watched, then the folders somebody
    // put there on purpose, then the machines those folders live on. The films on the set itself
    // come near the end, because on a television they are the rare case rather than the usual one,
    // and the channels are the floor -- a set with nothing else has those.
    val landingRow = opened?.first?.takeIf { it.isDrawn(state) } ?: when {
        state.recent.isNotEmpty() -> HomeRow.Recent
        state.folders.isNotEmpty() -> HomeRow.Folders
        state.servers.isNotEmpty() -> HomeRow.Servers
        state.videos.isNotEmpty() -> HomeRow.Videos
        else -> HomeRow.Channels
    }
    val landingKey = opened?.second?.takeIf { opened.first == landingRow }
    LaunchedEffect(landingRow, landingKey, state.servers) {
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

    // The last picture the focus crossed, kept while it stands on things that have none.
    //
    // A folder, a server and a channel list have no colour to give, and letting the room fall back
    // to its own ground for them meant the light came on and went out again on the way down the
    // screen -- which is the background flickering rather than the content being lit. Held, the
    // room keeps the colour until another picture changes it.
    //
    // The picture is held rather than the colour it gave: the reader below is keyed on the model
    // and answers from cache, so holding it costs one comparison and no second copy of anything.
    // It is also less work than before, not more -- the colour now changes when a picture is
    // crossed rather than twice for every row.
    var lit: Any? by remember { mutableStateOf(null) }
    LaunchedEffect(focused) { if (focused != null) lit = focused }

    // The colour is read from a 24-pixel copy of a picture that is already on screen, so it is a
    // cache hit and a scan of a few hundred pixels off the main thread.
    val surface = MaterialTheme.colorScheme.surface
    // What the room is before any picture has spoken, and on a set whose whole library is on a
    // share it may be that for good: a film on a share is never opened to draw a row, so there is
    // no frame to take a colour from.
    //
    // Neutral, and well under the tone of a card: the pools below peak at this colour, and a ground
    // that reached the cards' own grey would be a ground the cards sink into at the top of the
    // screen. A sixteenth of the foreground is enough to say the panel is lit and not enough to be
    // read as a colour.
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = BaseGlow).compositeOver(surface)
    val tint = rememberArtworkTint(model = lit, fallback = base)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Two pools of light set across the screen's diagonal, in the colour of whatever the
            // focus last found a picture on.
            //
            // Round rather than straight: a band is uniform along its whole width, which reads as
            // fog rather than as anything lighting a room, and it spends its few levels of grey
            // over the height of the screen alone -- a step of banding every fifteen pixels on a
            // large panel in the dark. Spread round a circle wider than the screen, the same levels
            // cover twice the distance in two directions.
            //
            // Two rather than one, and the second is why: a single pool reads as a lamp left on in
            // a corner, while two facing each other read as a room that is lit. It also costs the
            // banding rather than adding to it -- each pool spreads its steps over a different
            // distance, so where they overlap the steps of one fall between the steps of the other.
            //
            // Part of the colour and not all of it. What comes back from a cover has already been
            // pushed towards a dark, half-saturated version of itself, and it is still too much for
            // a wall behind text: a red sleeve lit the room like a warning lamp. Carrying about half
            // of it leaves the hue -- which is the whole of what a viewer reads at three metres --
            // and takes the intensity out. Where there is no cover the pools carry the ground's own
            // colour, which is the ground, so nothing is drawn twice for nothing.
            //
            // Three draws where there was one, and all three are a full screen of blended pixels a
            // television does without noticing. The layer is kept between changes anyway: it is
            // drawn again when the colour moves and not once in the seconds between.
            .drawBehind {
                drawRect(base)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = FirstStrength), Color.Transparent),
                        center = Offset(x = size.width * GlowCentre, y = 0f),
                        radius = size.height * GlowRadius,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = SecondStrength), Color.Transparent),
                        center = Offset(x = size.width * SecondCentre, y = size.height),
                        radius = size.height * SecondRadius,
                    ),
                )
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The two marks the phone keeps in its header, here for the same reason: they are things
            // to do rather than places to go. A place belongs in a row with the others; a thing to do
            // belongs above them, out of the way of the walk.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TvScreenInset, vertical = TvRowInset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Whose screen this is, at full strength and for as long as the screen is up. It
                // is the one word here that does not answer to anything: a name that dimmed as the
                // rows moved was a name that looked like it was going away.
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
                            landing = landing.takeIf { landingRow == HomeRow.Recent },
                            landingKey = landingKey,
                            onCardFocused = { viewModel.rememberOpened(HomeRow.Recent, it) },
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
                                    TvWatchedBar(entry.watched)
                                    TvCardDuration(entry.video.formattedDuration)
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
                                    TvWatchedBar(entry.watched)
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
                            landing = landing.takeIf { landingRow == HomeRow.Videos },
                            landingKey = landingKey,
                            onCardFocused = { viewModel.rememberOpened(HomeRow.Videos, it) },
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
                                // As in the row above and in the section itself: a film half
                                // watched is the same picture as one never opened, and this row
                                // holds the same films the row above holds when they are recent.
                                TvWatchedBar(video.playedPercentage.takeIf { it > 0f })
                                TvCardDuration(video.formattedDuration)
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
                            landing = landing.takeIf { landingRow == HomeRow.Folders },
                            landingKey = landingKey,
                            onCardFocused = { viewModel.rememberOpened(HomeRow.Folders, it) },
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
                        landing = landing.takeIf { landingRow == HomeRow.Servers },
                        landingKey = landingKey,
                        onCardFocused = { viewModel.rememberOpened(HomeRow.Servers, it) },
                        key = NetworkServerEntry::host,
                        trailing = { cardModifier ->
                            Tile(
                                title = stringResource(R.string.add_server),
                                onClick = { isAddingServer = true },
                                modifier = cardModifier,
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
                // Always, and with the way to add one on the end, exactly as the servers row has
                // it. Hidden when empty, the row went missing precisely when a viewer had no list
                // and needed to make one -- and the answer to that used to be a second Channels card
                // further down, which put the same place on this screen twice.
                item {
                    CardRow(
                        title = stringResource(R.string.channels),
                        items = state.playlists,
                        key = SavedPlaylist::url,
                        landing = landing.takeIf { landingRow == HomeRow.Channels },
                        landingKey = landingKey,
                        onCardFocused = { viewModel.rememberOpened(HomeRow.Channels, it) },
                        // Before the lists and not inside one of them. A starred channel belongs to
                        // the viewer rather than to the file it was found in, and the screen behind
                        // this card holds the starred of every list at once -- which is the whole
                        // reason it is here and not a heading in one of them.
                        leading = { cardModifier ->
                            // Always, even with nothing starred yet. A door that appears once you
                            // have found what is behind it is a door nobody finds: the card is how
                            // a viewer learns that starring a channel puts it somewhere.
                            Tile(
                                title = stringResource(R.string.favourites),
                                onClick = onOpenStarredChannels,
                                modifier = cardModifier,
                            ) { TvCardMark(VayouIcons.StarFilled) }
                        },
                        trailing = { cardModifier ->
                            Tile(
                                title = stringResource(R.string.add_playlist),
                                onClick = { isAddingPlaylist = true },
                                modifier = cardModifier,
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

                // The places, as cards, which is the whole of what the navigation used to be.
                //
                // A bar is a second thing to learn and a press to reach before anything can be chosen; four
                // cards at the foot of the rows a viewer is already walking cost nothing to find and nothing
                // to explain. Under the channels, because on a television the files on the set itself are the
                // rare case: what is above this row is what a viewer reaches for, and this is the rest.
                item {
                    CardRow(
                        title = stringResource(R.string.more),
                        items = Destinations,
                        key = Destination::label,
                        landing = landing.takeIf { landingRow == HomeRow.More },
                        landingKey = landingKey,
                        onCardFocused = { viewModel.rememberOpened(HomeRow.More, it) },
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
                face = action.face,
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
     * Handed to the row the focus is to land in, and null for every other.
     *
     * Without it the focus went to the rail, which then opened over the very screen the viewer had
     * just arrived at: a lazy column has composed nothing on the first frame, so the only thing
     * willing to take the focus was the navigation.
     */
    landing: FocusRequester? = null,
    /** Which card in it, or null to land on the first -- for a first visit, with nothing left. */
    landingKey: Any? = null,
    /** Called with the key of whichever card takes the focus, so the row can be come back to. */
    onCardFocused: (Any) -> Unit = {},
    /** A card before the first of them, for a row whose most-wanted thing is not one of them. */
    leading: (@Composable (Modifier) -> Unit)? = null,
    /** A card after the last of them, for a row that offers something as well as listing things. */
    trailing: (@Composable (Modifier) -> Unit)? = null,
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
            // The card that was left from, or, where there is none to come back to, the first
            // thing in the row -- a listed one if there is one, and otherwise whatever the row
            // offers instead. A key that no longer matches anything, a folder unpinned while away,
            // falls through to that same first card rather than to nothing at all.
            val defaultId: Any? = when {
                items.isNotEmpty() -> key(items.first())
                leading != null -> LeadingCard
                // Never the card at the end: those are the ones that add a machine or make a list,
                // and a screen that opens with the focus on one of them is a dialog waiting for the
                // second press of anybody who thought the first had not registered.
                else -> null
            }
            // Resolved once, and only for the row that owns the landing. A key that no longer
            // matches anything -- a folder unpinned while away, a machine forgotten, a film that
            // fell off the end of the row -- would otherwise attach the requester to nothing at
            // all, which is a screen the remote cannot use.
            val target = when {
                landing == null || landingKey == null -> defaultId
                landingKey == LeadingCard && leading != null -> landingKey
                landingKey == TrailingCard && trailing != null -> landingKey
                items.any { key(it) == landingKey } -> landingKey
                else -> defaultId
            }
            fun cardModifier(id: Any): Modifier = Modifier
                .onFocusChanged { if (it.isFocused) onCardFocused(id) }
                .then(
                    if (landing != null && id == target) {
                        Modifier.focusRequester(landing)
                    } else {
                        Modifier
                    },
                )

            leading?.let { content -> item(key = LeadingCard) { content(cardModifier(LeadingCard)) } }
            itemsIndexed(items, key = { _, item -> key(item) }) { _, item ->
                card(item, cardModifier(key(item)))
            }
            trailing?.let { content -> item(key = TrailingCard) { content(cardModifier(TrailingCard)) } }
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
        TvActions {
            TvAction(stringResource(R.string.cancel), onClick = onDismiss)
            // Absent until there is an address rather than greyed out: a D-pad walks past a
            // disabled control as if it were not there, so a dim one is a gap with a word in it.
            if (address.isNotBlank()) {
                TvAction(stringResource(R.string.connect)) { onAdd(address.trim()) }
            }
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
 * Says what this card looks like when the focus arrives on it, and nothing when it leaves.
 *
 * Only on arrival: a focus move is one card losing it and another taking it, and clearing on the
 * way out would blank the background for the frame in between, which reads as a flicker.
 */
private fun Modifier.reporting(artwork: Any?, onFocused: (Any?) -> Unit): Modifier =
    onFocusChanged { if (it.isFocused) onFocused(artwork) }

/**
 * Where the light comes from, and how far it carries.
 *
 * Over the head of the first card rather than the middle of the screen: centred, a glow is a lamp
 * behind the television and the eye finds it; off to the side it is only the room being lit. Its
 * middle sits on the top edge, so what is on screen is the lower half of it and never the bright
 * point itself.
 */
/** How lit the room is before any picture has given it a colour. */
private const val BaseGlow = 0.06f

/** How much of a cover's colour the near pool carries, and the far one after it. */
private const val FirstStrength = 0.55f

private const val SecondStrength = 0.25f

/** The second pool, set against the first across the diagonal. */
private const val SecondCentre = 0.88f

private const val SecondRadius = 1.2f

private const val GlowCentre = 0.28f

private const val GlowRadius = 1.5f

private val RowGap = 32.dp

/** Which row is on top, and so which card the focus lands on when the screen opens. */
/** The keys of the two cards a row can carry that are not one of the things it lists. */
private const val LeadingCard = "leading"

private const val TrailingCard = "trailing"

internal enum class HomeRow { Recent, Videos, Folders, Servers, More, Channels }

/**
 * Whether a row is on the screen at all, which decides whether it can be come back to.
 *
 * The three at the foot always are: each carries something to do -- add a machine, make a list --
 * so they are there for a television that has never been set up. The three above appear only when
 * they have something in them.
 */
private fun HomeRow.isDrawn(state: TvHomeState): Boolean = when (this) {
    HomeRow.Recent -> state.recent.isNotEmpty()
    HomeRow.Videos -> state.videos.isNotEmpty()
    HomeRow.Folders -> state.folders.isNotEmpty()
    HomeRow.Servers, HomeRow.More, HomeRow.Channels -> true
}

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

    /**
     * Drawn the way the card drew it, and not described so the menu can draw its own likeness.
     *
     * A mark named here is a second answer to what a thing looks like, and the two drifted: the row
     * showed the folder this app uses everywhere and the menu showed a grey outline of one. Carrying
     * the face itself, there is one answer and the menu cannot disagree with the card it came from.
     */
    val face: @Composable BoxScope.() -> Unit

    class Forget(val host: String, override val name: String) : HomeAction {
        override val face: @Composable BoxScope.() -> Unit = { TvCardMark(VayouIcons.Network) }
    }

    class Unpin(val folder: FavoriteFolder) : HomeAction {
        override val name = folder.displayName
        override val face: @Composable BoxScope.() -> Unit = { TvCardFolder() }
    }

    class RemoveList(val playlist: SavedPlaylist) : HomeAction {
        override val name = playlist.name
        override val face: @Composable BoxScope.() -> Unit = { TvCardMark(VayouIcons.Tv) }
    }
}
