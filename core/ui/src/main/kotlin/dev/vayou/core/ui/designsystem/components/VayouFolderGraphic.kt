package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A folder, sized by [width]; the height follows the folder's own proportions.
 *
 * One outline in two colours, rather than two shapes stacked. The front covers the whole silhouette
 * but the tab, so nothing sticks out past anything and there is no seam to align — earlier passes
 * that drew two independent panels spent themselves on exactly that.
 *
 * [folderColor] is the front, the panel that fills the shape and the one a reader would name if
 * asked the folder's colour. The tab comes from the theme beside it rather than from it: the step
 * between them is a shift in hue, and no tint of one colour arrives at another hue.
 *
 * No symbol on it. What a folder holds is already said by the tab it was opened from and by the rows
 * underneath, and a glyph there only competed with them.
 *
 * Amber, because that is what colour is for here. A folder sits interleaved with covers and
 * thumbnails, all of which carry their own colour; a grey shape among them reads as an image that
 * failed to load.
 */
@Composable
fun VayouFolderGraphic(modifier: Modifier = Modifier, folderColor: Color = VayouTheme.colors.accentFixed) {
    // The caller sets the width -- `Modifier.width(x)` where the size is known, `fillMaxWidth()` in
    // a grid cell whose width the grid decides. Taken as a Dp instead, a cell that measures itself
    // could not say what it wanted, which is what left the grid counting columns.
    Box(modifier = modifier) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.folder_thumb),
            contentDescription = null,
            tint = VayouTheme.colors.folderTabColor,
            modifier = Modifier.fillMaxWidth().aspectRatio(FolderAspectRatio),
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.folder_thumb_front),
            contentDescription = null,
            tint = folderColor,
            modifier = Modifier.fillMaxWidth().aspectRatio(FolderAspectRatio),
        )
    }
}

/** The drawing's own bounds, not the square an icon usually ships in. The margin around the shape
 *  would otherwise be measured by the row as part of the folder, and turn into a gap the other
 *  libraries do not have. */
private const val FolderAspectRatio = 20 / 17f

/**
 * The folder drawing on the grey tile a leading square is.
 *
 * A sheet's leading slot is a filled square everywhere else in the app -- a cover, a thumbnail, a
 * glyph on grey -- so a bare drawing floating in it read as a different kind of object from the
 * rows behind it. The two folder sheets disagreed about this in opposite directions: the network
 * one had the tile and drew a hollow glyph on it, the music one drew the folder and dropped the
 * tile. This is both halves: the tile, and the folder that is actually a folder.
 */
@Composable
fun VayouFolderTile(modifier: Modifier = Modifier, size: Dp = MediaListLayoutDefaults.LeadingSize) {
    Box(
        modifier = modifier
            .size(size)
            .clip(VayouTheme.shapes.medium)
            .background(VayouTheme.colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        VayouFolderGraphic(modifier = Modifier.width(size * FolderShare))
    }
}

/** As wide inside its tile as a glyph is: the same weight of mark, so a row of them keeps a rhythm. */
private const val FolderShare = 0.56f
