package dev.vayou.feature.player

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouSheetRow
import dev.vayou.core.ui.designsystem.components.VayouSheetRowIcon
import dev.vayou.core.ui.theme.VayouTheme

/**
 * One of a set, in any of the player's sheets.
 *
 * A tick at the leading edge, and the label the same colour whether or not it is the chosen one.
 * The sheets used to say this three ways -- amber text, amber text with a tick, and a tick alone
 * -- and the tick alone is the one that works: it is a shape rather than a hue, so it survives
 * colour blindness and a scrim laid over any frame of film, while colouring the label makes the
 * chosen row read as a different kind of text rather than as the same list.
 *
 * A tick and not the radio the settings use, because these sheets are drawn over film: a thin ring
 * over a moving frame reads as a smudge, and a tick survives any scene. The rows that are not chosen
 * hold its width, so every label starts on one column -- colour alone leaves nothing to line up.
 */
@Composable
internal fun CheckedRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    /** A fact about this row that is not its name -- the time a running timer has left. */
    trailing: String? = null,
) {
    VayouSheetRow(
        text = text,
        onClick = onClick,
        selected = isSelected,
        maxLines = 2,
        leading = {
            if (isSelected) {
                VayouSheetRowIcon(VayouIcons.Check, tint = VayouTheme.colors.accent)
            } else {
                Spacer(modifier = Modifier.size(VayouTheme.iconSize.md))
            }
        },
        trailing = trailing?.let {
            {
                Text(
                    text = it,
                    style = VayouTheme.typography.labelLarge,
                    color = VayouTheme.colors.accent,
                )
            }
        },
    )
}

/** A row that leads somewhere else, rather than one of a set. */
@Composable
internal fun MenuRow(icon: ImageVector, text: String, showChevron: Boolean = false, onClick: () -> Unit) {
    VayouSheetRow(
        text = text,
        onClick = onClick,
        leading = { VayouSheetRowIcon(icon) },
        trailing = if (showChevron) {
            { VayouSheetRowIcon(VayouIcons.ChevronRight, tint = VayouTheme.colors.onSurfaceVariant, isSmall = true) }
        } else {
            null
        },
    )
}

/** The rule between two kinds of row -- which one is on, where to get another, how they read. */
@Composable
internal fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = DividerSpacing),
        color = VayouTheme.colors.outlineVariant,
    )
}

/** Between a row's leading mark and its label, wherever a sheet draws one outside [VayouSheetRow]. */
internal val IconGap = 16.dp

private val DividerSpacing = 8.dp
