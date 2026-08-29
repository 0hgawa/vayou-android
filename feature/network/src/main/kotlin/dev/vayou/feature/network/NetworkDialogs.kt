package dev.vayou.feature.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.os.ConfigurationCompat
import dev.vayou.core.common.Utils
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.BrowserSortBy
import dev.vayou.core.smb.IptvCountries
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.ui.R as UiR
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.components.VayouBottomSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouCancelButton
import dev.vayou.core.ui.designsystem.components.VayouDialog
import dev.vayou.core.ui.designsystem.components.VayouDoneButton
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouSortOption
import dev.vayou.core.ui.designsystem.components.VayouSortSheet
import dev.vayou.core.ui.designsystem.components.VayouTextField
import dev.vayou.core.ui.theme.VayouTheme
import kotlinx.coroutines.delay

@Composable
internal fun AddServerDialog(onDismiss: () -> Unit, onConnect: (String) -> Unit) {
    var address by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.connect_to_server),
        confirmButton = {
            VayouDoneButton(enabled = address.isNotBlank(), onClick = { onConnect(address.trim()) })
        },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        VayouTextField(
            value = address,
            onValueChange = { address = it },
            label = stringResource(R.string.server_address),
            focusRequester = focus,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }

    FocusOnceOpen(focus)
}

@Composable
internal fun AddPlaylistDialog(onDismiss: () -> Unit, onAdd: (name: String, url: String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.add_playlist),
        confirmButton = {
            VayouDoneButton(enabled = url.isNotBlank(), onClick = { onAdd(name.trim(), url.trim()) })
        },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm)) {
            VayouTextField(
                value = url,
                onValueChange = { url = it },
                label = stringResource(R.string.playlist_url),
                focusRequester = focus,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            VayouTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.playlist_name_optional),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
    }

    FocusOnceOpen(focus)
}

@Composable
internal fun RenameDialog(name: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var newName by remember { mutableStateOf(name) }
    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.rename),
        confirmButton = {
            VayouDoneButton(enabled = newName.isNotBlank(), onClick = { onDone(newName.trim()) })
        },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        VayouTextField(
            value = newName,
            onValueChange = { newName = it },
            label = stringResource(R.string.name),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
}

@Composable
internal fun EditServerDialog(
    editing: EditingServer,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, password: String, domain: String) -> Unit,
) {
    var displayName by remember { mutableStateOf(editing.displayName) }
    var username by remember { mutableStateOf(editing.username) }
    var password by remember { mutableStateOf(editing.password) }

    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.edit_server),
        confirmButton = {
            VayouDoneButton(
                onClick = { onSave(displayName.trim(), username.trim(), password, editing.domain) },
            )
        },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm)) {
            Text(
                text = editing.host,
                style = VayouTheme.typography.bodySmall,
                color = VayouTheme.colors.onSurfaceVariant,
            )
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
        }
    }
}

@Composable
internal fun FileDetailsDialog(item: SmbFileItem, host: String?, share: String?, onDismiss: () -> Unit) {
    val location = listOfNotNull(host, share, item.path.replace('\\', '/').ifBlank { null })
        .joinToString("/")
    VayouDialog(
        onDismissRequest = onDismiss,
        title = item.name,
        confirmButton = { VayouDoneButton(onClick = onDismiss) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.md)) {
            if (item.size > 0) {
                DetailRow(stringResource(R.string.size), Utils.formatFileSize(item.size))
            }
            DetailRow(stringResource(R.string.location), location)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.xxs)) {
        Text(text = label, style = VayouTheme.typography.labelMedium, color = VayouTheme.colors.onSurfaceVariant)
        Text(text = value, style = VayouTheme.typography.bodyMedium, color = VayouTheme.colors.onSurface)
    }
}

@Composable
internal fun BrowserSortSheet(sort: BrowserSort, onChange: (BrowserSort) -> Unit, onDismiss: () -> Unit) {
    val axes = BrowserSortBy.entries
    VayouSortSheet(
        title = stringResource(R.string.sort),
        options = axes.map { axis ->
            VayouSortOption(
                label = stringResource(axis.label),
                icon = when (axis) {
                    BrowserSortBy.Name -> VayouIcons.Title
                    BrowserSortBy.Size -> VayouIcons.Size
                    BrowserSortBy.Type -> VayouIcons.FileOpen
                },
            )
        },
        selectedIndex = axes.indexOf(sort.by),
        isAscending = sort.isAscending,
        onSelect = { index ->
            val picked = axes[index]
            onChange(
                if (picked == sort.by) sort.copy(isAscending = !sort.isAscending) else sort.copy(by = picked),
            )
        },
        onDismiss = onDismiss,
    )
}

/**
 * Which country's channels to show.
 *
 * A sheet and not a dropdown: twenty-two entries in a box hanging off the app bar would scroll
 * inside themselves, while a sheet has the height the screen already has.
 */
@Composable
internal fun CountrySheet(currentCode: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = stringResource(R.string.country))
        val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]
        val everywhere = stringResource(UiR.string.iptv_international)
        LazyColumn(modifier = Modifier.heightIn(max = VayouSheetDefaults.ListMaxHeight)) {
            items(IptvCountries) { country ->
                FilterSheetRow(
                    text = country.nameIn(locale) ?: everywhere,
                    isSelected = country.code.orEmpty() == currentCode.orEmpty(),
                    onClick = { onSelect(country.code) },
                )
            }
        }
        Spacer(modifier = Modifier.height(VayouTheme.spacing.lg))
    }
}

/** The groups a playlist declares, which can be hundreds. */
@Composable
internal fun GroupFilterButton(groups: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    VayouIconButton(onClick = { isOpen = true }) {
        Icon(
            imageVector = VayouIcons.Filter,
            contentDescription = stringResource(R.string.filter_by_group),
            tint = if (selected != null) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
        )
    }
    if (isOpen) {
        VayouBottomSheet(onDismissRequest = { isOpen = false }) {
            VayouBottomSheetTitle(text = stringResource(R.string.filter_by_group))
            LazyColumn(modifier = Modifier.heightIn(max = VayouSheetDefaults.ListMaxHeight)) {
                item {
                    FilterSheetRow(
                        text = stringResource(R.string.all_groups),
                        isSelected = selected == null,
                    ) {
                        isOpen = false
                        onSelect(null)
                    }
                }
                items(groups) { group ->
                    FilterSheetRow(text = group, isSelected = selected == group) {
                        isOpen = false
                        onSelect(group)
                    }
                }
            }
            Spacer(modifier = Modifier.height(VayouTheme.spacing.lg))
        }
    }
}

/**
 * Focus once the dialog has finished arriving.
 *
 * Asked for on the first frame it is refused: the window is still animating in and has no focus of
 * its own yet, so the keyboard never opens and the field looks broken.
 */
@Composable
private fun FocusOnceOpen(focusRequester: FocusRequester) {
    LaunchedEffect(Unit) {
        delay(DialogEnterMs)
        focusRequester.requestFocus()
    }
}

private const val DialogEnterMs = 200L
