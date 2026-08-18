package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/**
 * What the tile is doing on the screen, which is the only thing that changes how loud its fallback
 * symbol should be.
 *
 * A named set rather than a size: given in dp at every call site it drifts into six different
 * shares of its tile -- 24dp inside a 48dp square on one row and inside a 56dp one on the next.
 */
enum class VayouArtworkRole(internal val iconFraction: Float, internal val iconAlpha: Float) {
    /** A list row, a grid cell, a queue entry: the symbol stands in for the missing cover. */
    Row(iconFraction = 0.45f, iconAlpha = 1f),

    /** The cover above a group's tracks. A row's share reads as oversized once the tile is this
     *  big, so the mark steps back and lets the shape carry it. */
    Hero(iconFraction = 0.4f, iconAlpha = 1f),

    /** The now-playing cover, where the artwork *is* the screen. Here the symbol has to read as an
     *  absence rather than as the thing being shown -- closer to a watermark. */
    Ghost(iconFraction = 0.28f, iconAlpha = 0.4f),
}

/**
 * The cover of something -- sized by [modifier], never by a parameter, so one component serves a
 * 40dp row and a full-width hero.
 *
 * The symbol is drawn *behind* the image rather than instead of it. MediaStore hands out an
 * album-art address for any track with an album id and most of them resolve to nothing; underneath,
 * the mark survives that, where an `if (uri == null)` would leave a blank square on most of a
 * library.
 */
@Composable
fun VayouArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    icon: ImageVector = VayouIcons.Audio,
    /**
     * Amber means *this opens into something*; a neutral means *this is a file that plays*.
     *
     * A folder, an album, an artist, a playlist, a server that answered -- tap it and you are
     * somewhere else, looking at a list. Those take the accent, and it stays worth noticing because
     * a list of them is seven rows.
     *
     * A track, a film, a file on a share -- tap it and it starts. Those take a neutral, because a
     * library of five hundred tinted alike is a wall rather than a signal.
     *
     * The glyph may take a colour at all because it sits inside a filled tile: decoration on a
     * shape, not a word that has to be read.
     */
    iconTint: Color = VayouTheme.colors.accent,
    /**
     * A letter to stand in for the cover, instead of [icon].
     *
     * For the things that have no cover and never will -- an artist is a name, not a record. The
     * letter is worth more than a symbol of a person, because it is the one under which the row is
     * filed: in an alphabetical list it says where you are, and a hundred identical silhouettes say
     * nothing at all.
     *
     * Sized for a row's tile. Nothing draws an initial at hero size, and a letter is not a glyph
     * that can be given a fraction of its box.
     */
    initial: String? = null,
    role: VayouArtworkRole = VayouArtworkRole.Row,
    shape: Shape = VayouTheme.shapes.medium,
    containerColor: Color = VayouTheme.colors.surfaceContainerHigh,
    /** Drawn over the artwork -- a queue marks what is playing this way. */
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                text = initial,
                style = VayouTheme.typography.titleLarge,
                color = iconTint.copy(alpha = role.iconAlpha),
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint.copy(alpha = role.iconAlpha),
                modifier = Modifier.fillMaxSize(role.iconFraction),
            )
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay()
    }
}
