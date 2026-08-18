package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * One named choice out of several, as a tile: outlined while it is not the one in use, filled while
 * it is.
 *
 * A tile rather than a chip where the set is laid out as a block and read rather than swiped -- the
 * equalizer's presets. A chip floats in a row and is sized by its word; a tile takes the width it is
 * given and holds its place in a grid, which is what lets ten of them be scanned.
 *
 * The label is centred, because a tile is sized by its row rather than by its word: "Flat" beside
 * "Heavy metal" on the leading edge is a ragged column of starts.
 */
@Composable
fun VayouSelectableTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /**
     * Drawn above the label, for sets whose members are told apart faster by a shape than by reading
     * four words. Without one the tile is the label alone, at the height it always was.
     */
    icon: ImageVector? = null,
) {
    val shape = VayouTheme.shapes.large
    // The target and the tile are two sizes. What a thumb has to hit stays at the 48dp floor; what
    // is drawn inside it is shorter, and the difference is transparent margin. Shrinking one would
    // have taken the other with it.
    //
    // Unless an icon and a word stack, which clears the floor on its own and so is drawn and hit at
    // the one height.
    val height = if (icon == null) VayouSelectableTileHeight else VayouSelectableTileWithIconHeight
    Box(
        modifier = modifier
            .height(if (icon == null) VayouSelectableTileTargetHeight else VayouSelectableTileWithIconHeight)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.background(VayouTheme.colors.onSurface)
                    } else {
                        Modifier.border(BorderWidth, VayouTheme.colors.outlineVariant, shape)
                    },
                )
                .padding(horizontal = VayouTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            val contentColor = when {
                selected -> VayouTheme.colors.surface
                enabled -> VayouTheme.colors.onSurface
                else -> VayouTheme.colors.onSurfaceVariant
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(IconLabelGap),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                }
                Text(
                    text = label,
                    style = VayouTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** What is drawn. Free to be as short as it reads well at, because it is not what has to be hit. */
val VayouSelectableTileHeight = 42.dp

/** What is hit: the floor this app holds for every target, and the height a row of these occupies,
 *  so shortening the tile did not close the gaps between them. */
val VayouSelectableTileTargetHeight = 48.dp

/** Taller, because an icon and a word stack. */
val VayouSelectableTileWithIconHeight = 64.dp

private val IconLabelGap = 6.dp

/** Tight enough that a block of them reads as one thing, wide enough that they do not touch. */
val VayouSelectableTileSpacing = 8.dp

private val BorderWidth = 1.dp
