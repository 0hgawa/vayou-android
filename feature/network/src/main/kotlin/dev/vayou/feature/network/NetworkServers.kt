package dev.vayou.feature.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.NetworkServerEntry
import dev.vayou.core.smb.SmbShare
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouConfirmButton
import dev.vayou.core.ui.designsystem.components.VayouEmptyState
import dev.vayou.core.ui.designsystem.components.VayouFolderTile
import dev.vayou.core.ui.designsystem.components.VayouListHeader
import dev.vayou.core.ui.designsystem.components.VayouTextField
import dev.vayou.core.ui.theme.VayouTheme

/** What is on this network, saved or found, with the pinned folders above it. */
@Composable
internal fun ServerList(
    isLoading: Boolean,
    error: NetworkError?,
    servers: List<NetworkServerEntry>,
    favouriteFolderCount: Int,
    onServerClick: (NetworkServerEntry) -> Unit,
    onEditServer: (NetworkServerEntry) -> Unit,
    onForgetServer: (NetworkServerEntry) -> Unit,
    onOpenFolderFavourites: () -> Unit,
) {
    val online = stringResource(R.string.online)
    val offline = stringResource(R.string.offline)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (error != null) {
            item { InlineError(error) }
        }

        item(key = "folder_favourites") {
            NetworkRow(
                icon = { NetworkTile(VayouIcons.Pin) },
                title = stringResource(R.string.folder_favourites),
                subtitle = pluralStringResource(R.plurals.n_folders, favouriteFolderCount, favouriteFolderCount),
                onClick = onOpenFolderFavourites,
            )
        }

        if (servers.isEmpty() && !isLoading) {
            item { VayouEmptyState(VayouIcons.Wifi, stringResource(R.string.no_servers_found)) }
        }

        items(servers, key = { it.host }) { entry ->
            // Every other row in the app carries a second line; without one these sit taller and
            // lighter than the same row one tab over.
            val subtitle = listOfNotNull(
                entry.host.takeIf { it != entry.displayName },
                if (entry.isOnline) online else offline,
            ).joinToString(Separator)

            NetworkRow(
                icon = {
                    NetworkTile(
                        icon = if (entry.isSaved) VayouIcons.Network else VayouIcons.Wifi,
                        tint = if (entry.isOnline) {
                            VayouTheme.colors.accent
                        } else {
                            VayouTheme.colors.onSurfaceVariant.copy(alpha = OfflineAlpha)
                        },
                    )
                },
                title = entry.displayName,
                subtitle = subtitle,
                onClick = { onServerClick(entry) },
                trailingContent = if (!entry.isSaved) {
                    null
                } else {
                    {
                        ItemOverflowMenu(
                            name = entry.displayName,
                            subtitle = subtitle,
                            onRename = { onEditServer(entry) },
                            onRemove = { onForgetServer(entry) },
                            leading = { NetworkTile(VayouIcons.Network) },
                        )
                    }
                },
            )
        }
    }
}

@Composable
internal fun ShareList(
    host: String,
    isLoading: Boolean,
    error: NetworkError?,
    shares: List<SmbShare>,
    favouritedShares: Set<String>,
    onShareClick: (SmbShare) -> Unit,
    onToggleFavourite: (SmbShare) -> Unit,
) {
    when {
        isLoading -> Waiting()
        error != null -> ErrorState(error)
        shares.isEmpty() -> VayouEmptyState(VayouIcons.Folder, stringResource(R.string.no_shares_found))
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { VayouListHeader(label = host) }
            items(shares, key = { it.name }) { share ->
                NetworkRow(
                    icon = { NetworkFolderGraphic() },
                    title = share.name,
                    onClick = { onShareClick(share) },
                    trailingContent = {
                        FolderFavouriteMenu(
                            name = share.name,
                            isFavourite = share.name in favouritedShares,
                            onToggle = { onToggleFavourite(share) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun FolderFavourites(
    favourites: List<FavoriteFolder>,
    /**
     * What came of the last attempt to open one of these, or null.
     *
     * Shown here rather than swallowed: a pinned folder whose machine is asleep used to send the
     * viewer back to this list with nothing said, and the reason only appeared on the screen before
     * it. The pin itself stays -- it is a shortcut the viewer made, and the machine being off is
     * this evening's fact, not the shortcut's.
     */
    error: NetworkError?,
    onOpen: (FavoriteFolder) -> Unit,
    onRename: (FavoriteFolder) -> Unit,
    onRemove: (FavoriteFolder) -> Unit,
) {
    if (favourites.isEmpty()) {
        VayouEmptyState(VayouIcons.Pin, stringResource(R.string.no_pinned_folders))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (error != null) {
            item { InlineError(error) }
        }

        items(favourites, key = { "${it.host}|${it.share}|${it.path}" }) { favourite ->
            NetworkRow(
                icon = { NetworkFolderGraphic() },
                title = favourite.displayName,
                subtitle = "${favourite.host}$Separator${favourite.share}/${favourite.path.replace('\\', '/')}"
                    .trimEnd('/'),
                onClick = { onOpen(favourite) },
                trailingContent = {
                    ItemOverflowMenu(
                        name = favourite.displayName,
                        subtitle = favourite.host,
                        onRename = { onRename(favourite) },
                        onRemove = { onRemove(favourite) },
                        leading = { VayouFolderTile() },
                    )
                },
            )
        }
    }
}

/**
 * The password box, shown only once a guest connection has already been refused.
 *
 * A whole screen and not a dialog: three fields and an error under them is more than a dialog holds
 * well, and this is a step in reaching a server rather than an interruption of something else.
 */
@Composable
internal fun AuthForm(
    host: String,
    isLoading: Boolean,
    error: NetworkError?,
    onSubmit: (username: String, password: String, displayName: String) -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(VayouTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.authentication_required),
            style = VayouTheme.typography.titleMedium,
            color = VayouTheme.colors.onSurface,
        )
        Text(text = host, style = VayouTheme.typography.bodyMedium, color = VayouTheme.colors.onSurfaceVariant)

        VayouTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = stringResource(R.string.server_name_optional),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        VayouTextField(
            value = username,
            onValueChange = { username = it },
            label = stringResource(R.string.username),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        VayouTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            visualTransformation = PasswordVisualTransformation(),
        )

        if (error != null) {
            Text(
                text = stringResource(error.label),
                style = VayouTheme.typography.bodySmall,
                color = VayouTheme.colors.error,
            )
        }

        VayouConfirmButton(
            onClick = { onSubmit(username, password, displayName) },
            enabled = !isLoading && username.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) {
                // The button's own content colour, so the spinner follows the container.
                VayouCircularProgress(
                    size = SpinnerInButton,
                    strokeWidth = ThinStroke,
                    color = LocalContentColor.current,
                )
            } else {
                Text(text = stringResource(R.string.connect), style = VayouTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun Waiting() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        VayouCircularProgress()
    }
}

@Composable
internal fun ErrorState(error: NetworkError) {
    VayouEmptyState(
        icon = VayouIcons.Priority,
        title = stringResource(error.label),
        iconTint = VayouTheme.colors.error,
    )
}

/** An error that does not replace the list, because the list is still worth showing. */
@Composable
private fun InlineError(error: NetworkError) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VayouTheme.spacing.lg, vertical = VayouTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
    ) {
        Icon(
            imageVector = VayouIcons.Priority,
            contentDescription = null,
            tint = VayouTheme.colors.error,
            modifier = Modifier.size(VayouTheme.iconSize.sm),
        )
        Text(
            text = stringResource(error.label),
            style = VayouTheme.typography.bodyMedium,
            color = VayouTheme.colors.error,
        )
    }
}

internal val NetworkError.label: Int
    get() = when (this) {
        NetworkError.NotOnThisNetwork -> R.string.error_not_on_this_network
        NetworkError.WrongCredentials -> R.string.error_wrong_credentials
        NetworkError.CannotList -> R.string.error_cannot_list
        NetworkError.PlaylistUnreachable -> R.string.error_playlist_unreachable
    }

internal const val Separator = " · "

private const val OfflineAlpha = 0.5f

private val SpinnerInButton = 18.dp

private val ThinStroke = 2.dp
