package dev.vayou.feature.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouActionSheet
import dev.vayou.core.ui.designsystem.components.VayouActionSheetItem
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouFolderGraphic
import dev.vayou.core.ui.designsystem.components.VayouFolderTile
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouOverflowButton
import dev.vayou.core.ui.designsystem.components.VayouSegmentedListItem
import dev.vayou.core.ui.designsystem.components.VayouSelectionMark
import dev.vayou.core.ui.theme.VayouTheme

/** Every row in this section, so a server, a share, a folder and a file all read as one list. */
@Composable
internal fun NetworkRow(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    VayouSegmentedListItem(
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        onClick = onClick,
        leadingContent = icon,
        content = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = subtitle?.let { { Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        trailingContent = trailingContent,
    )
}

/**
 * The same folder the local library draws, at the size the network rows use.
 *
 * A share and a directory are folders like any other; drawn as a flat glyph in a box, this section
 * looked like a different app one screen over.
 */
@Composable
internal fun NetworkFolderGraphic() {
    VayouFolderGraphic(modifier = Modifier.width(MediaListLayoutDefaults.LeadingSize))
}

@Composable
internal fun NetworkTile(icon: ImageVector, tint: Color? = null) {
    VayouArtwork(
        model = null,
        modifier = Modifier.size(MediaListLayoutDefaults.LeadingSize),
        icon = icon,
        iconTint = tint ?: VayouTheme.colors.accent,
    )
}

/** Rename or remove, for anything the viewer added themselves. */
@Composable
internal fun ItemOverflowMenu(
    name: String,
    subtitle: String?,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    /** The same square the row leads with, so the sheet reads as that row opened. */
    leading: @Composable () -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }
    VayouOverflowButton(onClick = { isOpen = true }, tint = VayouTheme.colors.onSurfaceVariant)
    if (isOpen) {
        VayouActionSheet(
            title = name,
            subtitle = subtitle,
            onDismiss = { isOpen = false },
            leading = leading,
        ) {
            VayouActionSheetItem(stringResource(R.string.rename), VayouIcons.Edit) {
                isOpen = false
                onRename()
            }
            VayouActionSheetItem(stringResource(R.string.remove), VayouIcons.Delete) {
                isOpen = false
                onRemove()
            }
        }
    }
}

/** Pin a share or a directory, so a deep path is one tap from the server list. */
@Composable
internal fun FolderFavouriteMenu(name: String, isFavourite: Boolean, onToggle: () -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    VayouOverflowButton(onClick = { isOpen = true }, tint = VayouTheme.colors.onSurfaceVariant)
    if (isOpen) {
        VayouActionSheet(title = name, onDismiss = { isOpen = false }, leading = { VayouFolderTile() }) {
            VayouActionSheetItem(
                text = stringResource(if (isFavourite) R.string.unpin_folder else R.string.pin_folder),
                icon = VayouIcons.Pin,
            ) {
                isOpen = false
                onToggle()
            }
        }
    }
}

@Composable
internal fun VideoActionsMenu(
    name: String,
    subtitle: String?,
    onPlayFromStart: () -> Unit,
    onShowDetails: () -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }
    VayouOverflowButton(onClick = { isOpen = true }, tint = VayouTheme.colors.onSurfaceVariant)
    if (isOpen) {
        VayouActionSheet(
            title = name,
            subtitle = subtitle,
            onDismiss = { isOpen = false },
            leading = { NetworkTile(VayouIcons.VideoFilled, VayouTheme.colors.onSurfaceVariant) },
        ) {
            VayouActionSheetItem(stringResource(R.string.play_from_start), VayouIcons.Play) {
                isOpen = false
                onPlayFromStart()
            }
            VayouActionSheetItem(stringResource(R.string.details), VayouIcons.Info) {
                isOpen = false
                onShowDetails()
            }
        }
    }
}

@Composable
internal fun ChannelRow(
    channel: PlaylistChannel,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
) {
    VayouSegmentedListItem(
        selected = isSelected,
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            VayouSelectionMark(selected = isSelecting && isSelected) { ChannelLogo(channel.logo) }
        },
        content = { Text(text = channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        // The star goes while a selection is running: the toolbar stars the whole set, and two ways
        // to star from one screen disagree the moment a mixed selection is picked.
        trailingContent = if (isSelecting) {
            null
        } else {
            {
                VayouIconButton(onClick = onToggleFavourite) {
                    Icon(
                        imageVector = if (isFavourite) VayouIcons.StarFilled else VayouIcons.StarOutlined,
                        contentDescription = null,
                        tint = if (isFavourite) VayouTheme.colors.accent else VayouTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                }
            }
        },
    )
}

/** A broadcaster's mark is drawn wide, so this is a 4:3 box rather than the square the files use. */
@Composable
private fun ChannelLogo(logo: String?) {
    Box(
        modifier = Modifier
            .width(MediaListLayoutDefaults.LeadingSize)
            .aspectRatio(LogoAspect)
            .clip(VayouTheme.shapes.small)
            .background(VayouTheme.colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (logo == null) {
            Icon(
                imageVector = VayouIcons.Play,
                contentDescription = null,
                tint = VayouTheme.colors.onSurfaceVariant.copy(alpha = FallbackAlpha),
                modifier = Modifier.size(FallbackGlyph),
            )
        } else {
            // Fit, not crop: a logo is a mark on a background, and cropping one cuts the name off it.
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** One choice in a filter sheet -- the country list and the group list pick the same way. */
@Composable
internal fun FilterSheetRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VayouTheme.spacing.xl, vertical = VayouTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
    ) {
        Text(
            text = text,
            style = VayouTheme.typography.bodyLarge,
            color = VayouTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = VayouIcons.Check,
                contentDescription = null,
                tint = VayouTheme.colors.accent,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
            )
        }
    }
}

/**
 * What a file in a share is, by its extension.
 *
 * A share holds whatever someone put in it -- covers, subtitles, notes -- and drawing all of them as
 * video made every row look playable when only some are.
 */
internal fun SmbFileItem.kindIcon(): ImageVector = when {
    isVideo -> VayouIcons.Video
    isAudio -> VayouIcons.Audio
    isSubtitle -> VayouIcons.Subtitle
    extension in ImageExtensions -> VayouIcons.Image
    else -> VayouIcons.FileOpen
}

private val ImageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")

/** The square the network rows lead with, matching the library's own leading artwork. */
/** The same box a sheet's header uses, so a row and the sheet it opens are one size. */
private const val LogoAspect = 4f / 3f

private const val FallbackAlpha = 0.5f

private val FallbackGlyph = 18.dp
