package dev.vayou.tv.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.common.Utils
import dev.vayou.core.smb.BrowserSortBy
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.tv.R
import dev.vayou.tv.TvCard
import dev.vayou.tv.TvCardFolder
import dev.vayou.tv.TvCardGap
import dev.vayou.tv.TvCardMark
import dev.vayou.tv.TvCardWidth
import dev.vayou.tv.TvChoiceList
import dev.vayou.tv.TvDetails
import dev.vayou.tv.TvMessage
import dev.vayou.tv.TvOptionItem
import dev.vayou.tv.TvOptions
import dev.vayou.tv.TvOrderButton
import dev.vayou.tv.TvRowInset
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvSearchHeader
import dev.vayou.tv.TvTextField
import dev.vayou.tv.TvTile
import dev.vayou.tv.TvTitleInset
import kotlinx.coroutines.launch

/**
 * What is on a machine down the hall.
 *
 * One screen for the whole walk rather than one per level: on a television the shares and the
 * folders inside them look and behave the same, and pushing a route for each step would put a
 * dozen entries in the back stack for one film.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvServerScreen(
    onPlayVideo: (String) -> Unit,
    onPlayAudio: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TvServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pinned by viewModel.pinned.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    /** The share or folder whose options are up: its path within the share, and what to call it. */
    var acting by remember { mutableStateOf<HeldItem?>(null) }

    /** The file whose details are up. Read only: nothing there is a thing to press. */
    var showing by remember { mutableStateOf<SmbFileItem?>(null) }

    /** What is being looked for in this folder, or null while the viewer is walking it. */
    var query by rememberSaveable { mutableStateOf<String?>(null) }

    // This folder and not the whole share: a share can hold thousands of files across a tree, and
    // reading all of them to answer would be a walk of the machine rather than a search of a screen.
    val shown = remember(state.entries, query) {
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { needle ->
            state.entries.filter { it.name.contains(needle, ignoreCase = true) }
        } ?: state.entries
    }

    var isChoosingOrder by remember { mutableStateOf(false) }

    // Handed to the first card whenever a new listing arrives. A television screen with nothing
    // focused is a screen the remote cannot use: every press goes nowhere and it reads as frozen.
    val firstCard = remember { FocusRequester() }
    LaunchedEffect(state.shares, state.entries) {
        if (state.shares.isNotEmpty() || state.entries.isNotEmpty()) runCatching { firstCard.requestFocus() }
    }

    // Back puts the keyboard away, then walks up the folders, and only then leaves. Three steps and
    // not one: a viewer six levels in should not be thrown all the way out for pressing it once, and
    // one who has just typed a password should not lose the screen for dismissing the keyboard they
    // are looking at. The television's keyboard is drawn over this app rather than by it, so nothing
    // else was going to consume that press.
    val keyboard = LocalSoftwareKeyboardController.current
    val isTyping = WindowInsets.isImeVisible
    BackHandler {
        when {
            isTyping -> keyboard?.hide()
            query != null -> query = null
            viewModel.goUp() -> Unit
            else -> onBack()
        }
    }

    if (isChoosingOrder) {
        TvChoiceList(
            title = stringResource(R.string.sort),
            options = BrowserSortBy.entries.map {
                it to stringResource(it.label)
            },
            selected = state.sort.by,
            onChoose = viewModel::selectSort,
            onDismiss = { isChoosingOrder = false },
        )
    }

    showing?.let { file ->
        TvDetails(
            title = file.name,
            lines = listOfNotNull(
                stringResource(R.string.info_file) to file.name,
                stringResource(R.string.info_location) to "${state.share.orEmpty()}\\${file.path}",
                file.size.takeIf { it > 0 }?.let { stringResource(R.string.info_size) to Utils.formatFileSize(it) },
            ),
            onDismiss = { showing = null },
        )
    }

    acting?.let { held ->
        // A folder is a place to keep and a file is a thing to play, so the two are asked different
        // questions -- but they are held the same way, and one menu draws both.
        val file = held.file
        val share = if (held.path.isEmpty()) held.name else state.share.orEmpty()
        val isPinned = keyOf(share, held.path) in pinned
        TvOptions(
            face = {
                when {
                    file?.isDirectory == false ->
                        TvCardMark(if (file.isAudio) VayouIcons.Audio else VayouIcons.Video)

                    held.path.isEmpty() -> TvCardMark(VayouIcons.Network)
                    else -> TvCardFolder()
                }
            },
            title = held.name,
            onDismiss = { acting = null },
            options = if (file != null && !file.isDirectory) {
                listOf(TvOptionItem(VayouIcons.Info, stringResource(R.string.details)) { showing = file })
            } else {
                listOf(
                    TvOptionItem(
                        icon = VayouIcons.Pin,
                        label = stringResource(if (isPinned) R.string.unpin_folder else R.string.pin_folder),
                    ) { viewModel.togglePinned(share, held.path, held.name) },
                )
            },
        )
    }

    // Opaque, because the screen it is pushed over is still drawn underneath while the two are
    // being swapped: through a transparent one, the cards of the screen behind show as a shadow
    // across this one.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        TvSearchHeader(
            title = state.here(viewModel.host),
            query = query,
            onSearch = { query = it },
            onOpenSearch = { query = "" },
            // The remote has a back key and everyone knows it, which is why this is here too: the
            // key is invisible, and a screen six folders deep should show the way out rather than
            // ask to be remembered.
            onBack = { if (!viewModel.goUp()) onBack() },
            // Not over the list of shares: a machine offers three or four of them and they are
            // ordered by the only thing a share has, which is its name.
            action = if (state.share == null) {
                null
            } else {
                {
                    TvOrderButton(
                        isAscending = state.sort.isAscending,
                        label = stringResource(R.string.sort),
                    ) { isChoosingOrder = true }
                }
            },
        )

        when {
            state.isLoading -> TvMessage(stringResource(R.string.reaching_server))
            state.needsSignIn -> SignIn(state.hasFailed, viewModel::signIn)
            state.hasFailed -> TvMessage(stringResource(R.string.server_unreachable))
            state.shares.isEmpty() && shown.isEmpty() ->
                TvMessage(stringResource(if (query == null) R.string.nothing_here else R.string.nothing_found))
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(TvCardWidth),
                contentPadding = PaddingValues(horizontal = TvScreenInset, vertical = TvTitleInset),
                horizontalArrangement = Arrangement.spacedBy(TvCardGap),
                verticalArrangement = Arrangement.spacedBy(TvCardGap),
            ) {
                itemsIndexed(state.shares, key = { _, share -> share.name }) { index, share ->
                    TvTile(
                        title = share.name,
                        onClick = { viewModel.openShare(share) },
                        // Held rather than tapped, as starring a film is: what can be done to a
                        // folder is not worth a button on every card.
                        onLongClick = { acting = HeldItem(share.name, "") },
                        modifier = if (index == 0) Modifier.focusRequester(firstCard) else Modifier,
                    ) {
                        TvCardMark(VayouIcons.Network)
                        if (keyOf(share.name, "") in pinned) Pin()
                    }
                }
                itemsIndexed(shown, key = { _, entry -> entry.path }) { index, entry ->
                    // A folder is a place and a file is a thing to play, so they are not the same
                    // card: one carries its name inside, the other under the mark that says what
                    // kind of file it is.
                    val landing = if (index == 0 && state.shares.isEmpty()) {
                        Modifier.focusRequester(firstCard)
                    } else {
                        Modifier
                    }
                    // Resolved before the player is opened: the address only means anything once the
                    // share is connected and open for reading. A share holds both kinds and each has
                    // a screen of its own, as it does on the phone -- a track opened in the film
                    // player is a black rectangle with a seek bar under it.
                    val open = {
                        scope.launch {
                            val address = viewModel.addressOf(entry) ?: return@launch
                            if (entry.isAudio) onPlayAudio(address) else onPlayVideo(address)
                        }
                        Unit
                    }

                    if (entry.isDirectory) {
                        TvTile(
                            title = entry.name,
                            onClick = { viewModel.openDirectory(entry) },
                            onLongClick = { acting = HeldItem(entry.name, entry.path, entry) },
                            modifier = landing,
                        ) {
                            TvCardFolder()
                            if (keyOf(state.share.orEmpty(), entry.path) in pinned) Pin()
                        }
                    } else {
                        TvCard(
                            title = entry.name,
                            onClick = open,
                            // Held rather than tapped, as everywhere else: what a file can tell you
                            // about itself is not worth a button on every card.
                            onLongClick = { acting = HeldItem(entry.name, entry.path, entry) },
                            modifier = landing,
                        ) {
                            TvCardMark(if (entry.isAudio) VayouIcons.Audio else VayouIcons.Video)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The mark that says this folder is on the home screen.
 *
 * In the corner rather than in the middle, because a folder still has to look like a folder: the
 * mark answers a second question, and answering it by replacing the first would be a worse card.
 */
@Composable
private fun BoxScope.Pin() {
    Icon(
        imageVector = VayouIcons.Pin,
        contentDescription = stringResource(R.string.pinned),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(TvRowInset)
            .size(PinSize),
    )
}

/** As big as the mark a card carries in its other corner. */
private val PinSize = 24.dp

/** Where the viewer is: the machine at the top, the folder they walked into once they have. */
@Composable
private fun TvServerState.here(host: String): String = when {
    path.isNotEmpty() -> path.trimEnd('\\').substringAfterLast('\\')
    share != null -> share
    else -> host
}

/**
 * A name and a password, typed with whatever keyboard the television puts on screen.
 *
 * Asked here and nowhere else, and asked once per machine because the answer is kept. A television
 * has no keyboard of its own, so this is the one thing worth making a viewer type.
 */
@Composable
private fun SignIn(hasFailed: Boolean, onSubmit: (String, String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FieldGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (hasFailed) R.string.sign_in_failed else R.string.sign_in),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Held to a column in the middle. A field is as wide as the answer it wants, and a name
        // stretched across two metres of television reads as a mistake rather than as a form.
        TvTextField(
            value = username,
            onValueChange = { username = it },
            label = stringResource(R.string.username),
            modifier = Modifier
                .width(FieldWidth)
                .focusRequester(first),
        )
        TvTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password),
            modifier = Modifier.width(FieldWidth),
            isSecret = true,
        )
        Surface(
            onClick = { onSubmit(username, password) },
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            // Focus turns this white, as it turns every other control on the television white --
            // the rows, the icon buttons, the transport. Taking the primary made this the one amber
            // thing on a screen of them, which reads as a different kind of button rather than as
            // the one that happens to be focused. On this shell primary is the mark's fixed amber,
            // so it shouts louder here than the same choice would on the phone.
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                focusedContentColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text(
                text = stringResource(R.string.connect),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = TvScreenInset, vertical = FieldPadding),
            )
        }
    }
}

private val FieldGap = 16.dp

private val FieldWidth = 480.dp

private val FieldPadding = 12.dp

private val FocusRing = 2.dp

/** How each axis is named here. The store keeps the axis; the words belong to whoever draws it. */
private val BrowserSortBy.label: Int
    get() = when (this) {
        BrowserSortBy.Name -> R.string.sort_by_title
        BrowserSortBy.Size -> R.string.sort_by_size
        BrowserSortBy.Type -> R.string.sort_by_type
    }

/** A card held down: what it is called, where it lives, and the entry itself when it is one. */
private class HeldItem(val name: String, val path: String, val file: SmbFileItem? = null)
