package dev.vayou.feature.music

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.vayou.core.media.MediaTags
import dev.vayou.core.media.Song
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouArtworkRole
import dev.vayou.core.ui.designsystem.components.VayouBackButton
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouConfirmButton
import dev.vayou.core.ui.designsystem.components.VayouTextButton
import dev.vayou.core.ui.designsystem.components.VayouTextField
import dev.vayou.core.ui.designsystem.components.VayouTopAppBar
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Corrects what a track says it is.
 *
 * Full screen rather than a sheet: five fields and a cover do not fit a sheet without scrolling
 * inside a box, and typing wants the room once the keyboard is up.
 *
 * Save stays off until something actually changes, so the button doubles as the answer to "did I
 * edit anything?".
 */
@Composable
internal fun TagEditor(
    song: Song,
    /** True while the file is being written: the form stays up and says so. */
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (MediaTags, Uri?) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var title by remember { mutableStateOf(song.title) }
        var artist by remember { mutableStateOf(song.artist) }
        var album by remember { mutableStateOf(song.album) }
        // The library reads neither of these from the file, so they open on what can be guessed
        // and are only written when the listener puts something in them.
        var albumArtist by remember { mutableStateOf(song.artist) }
        var year by remember { mutableStateOf("") }

        // Shown at once, so the choice is visible before the file has been written and rescanned.
        var pickedCover by remember { mutableStateOf<Uri?>(null) }
        val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
            if (picked != null) pickedCover = picked
        }

        val isEdited = title != song.title ||
            artist != song.artist ||
            album != song.album ||
            albumArtist != song.artist ||
            year.isNotBlank() ||
            pickedCover != null

        BackHandler(onBack = onDismiss)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VayouTheme.colors.surface)
                .imePadding(),
        ) {
            VayouTopAppBar(
                title = stringResource(R.string.edit_tags),
                navigationIcon = { VayouBackButton(onClick = onDismiss) },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = VayouTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FieldGap),
            ) {
                VayouArtwork(
                    model = pickedCover ?: song.artworkUri,
                    iconTint = VayouTheme.colors.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = VayouTheme.spacing.xl)
                        .size(CoverSize),
                    role = VayouArtworkRole.Hero,
                    shape = VayouTheme.shapes.large,
                )
                VayouTextButton(
                    onClick = {
                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
                    ) {
                        Icon(imageVector = VayouIcons.Image, contentDescription = null)
                        Text(text = stringResource(R.string.change_cover))
                    }
                }

                TagField(R.string.tag_title, title) { title = it }
                TagField(R.string.tag_artist, artist) { artist = it }
                TagField(R.string.tag_album, album) { album = it }
                TagField(R.string.tag_album_artist, albumArtist) { albumArtist = it }
                TagField(R.string.tag_year, year, isNumeric = true) { year = it }

                // At the foot of the form rather than in the bar: the last thing to do is the last
                // thing on the screen, and after the final field the thumb is already down here.
                VayouConfirmButton(
                    onClick = {
                        onSave(
                            MediaTags(
                                title = title.trim(),
                                artist = artist.trim(),
                                album = album.trim(),
                                albumArtist = albumArtist.trim(),
                                year = year.trim(),
                            ),
                            pickedCover,
                        )
                    },
                    enabled = isEdited && title.isNotBlank() && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = VayouTheme.spacing.xl),
                ) {
                    if (isSaving) {
                        VayouCircularProgress(size = SpinnerSize)
                    } else {
                        Text(text = stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
private fun TagField(labelRes: Int, value: String, isNumeric: Boolean = false, onValueChange: (String) -> Unit) {
    VayouTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(labelRes),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isNumeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
    )
}

/** Big enough to judge a cover by, short enough to leave the first field above the keyboard. */
private val CoverSize = 180.dp

private val FieldGap = 20.dp

/** The height of the label it stands in for, so the button does not change size mid-press. */
private val SpinnerSize = 20.dp
