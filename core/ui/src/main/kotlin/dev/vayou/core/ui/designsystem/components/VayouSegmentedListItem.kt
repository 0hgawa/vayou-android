package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

@Composable
fun PreferenceGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VayouSegmentedListItem(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = VayouListItemPadding,
    containerColor: Color = Color.Transparent,
    rippleColor: Color = Color.Unspecified,
    contentColorOverride: Color? = null,
    supportingColorOverride: Color? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val bgColor = if (selected) VayouTheme.colors.onSurface.copy(alpha = 0.12f) else containerColor
    // Picked is said by the plate behind the row and by the mark on its artwork. Recolouring the
    // text as well is a third voice saying one thing, and the role it used to take -- the ink that
    // belongs on an accent container -- is a brown once it lands on this grey rather than on that
    // container. A name is the same name whether or not it is ticked.
    val contentColor = contentColorOverride ?: when {
        enabled -> VayouTheme.colors.onSurface
        else -> VayouTheme.colors.onDisabled
    }
    val subColor = supportingColorOverride ?: when {
        enabled -> VayouTheme.colors.onSurfaceVariant
        else -> VayouTheme.colors.onDisabled
    }

    val source = interactionSource ?: remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(bgColor)
            .combinedClickable(
                interactionSource = source,
                indication = ripple(color = rippleColor),
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        // 12dp, not the M3 default 16: with a 48dp cover the wider gap reads as a gutter and pushes
        // the title off the optical grid the artwork sets up.
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leadingContent != null) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                leadingContent()
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (overlineContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides subColor,
                    LocalTextStyle provides VayouTheme.typography.labelMedium,
                ) {
                    overlineContent()
                }
            }
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides VayouTheme.typography.bodyLarge,
            ) {
                content()
            }
            if (supportingContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides subColor,
                    LocalTextStyle provides VayouTheme.typography.bodyMedium,
                ) {
                    supportingContent()
                }
            }
        }

        if (trailingContent != null) {
            CompositionLocalProvider(LocalContentColor provides subColor) {
                trailingContent()
            }
        }
    }
}

@Composable
fun ListSectionTitle(
    modifier: Modifier = Modifier,
    text: String,
    contentPadding: PaddingValues = PaddingValues(
        start = 24.dp,
        top = 20.dp,
        bottom = 10.dp,
    ),
    /**
     * The quiet role, not the accent.
     *
     * A heading names what is under it; the accent is for what the reader can act on. Colouring
     * both the same makes a page of settings read as a page of links.
     */
    color: Color = VayouTheme.colors.onSurfaceVariant,
) {
    Text(
        text = text,
        modifier = modifier.padding(contentPadding),
        color = color,
        style = VayouTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The margin a settings row starts its content on. Named so that anything laid out beside those
 *  rows -- a block of tiles, a chart -- can line up with them instead of guessing the number. */
val VayouListItemInset = 32.dp

/** What a row on a screen sits inside. Named so that a caller wanting a different margin -- a sheet,
 *  which is narrower than the screen behind it -- can say so instead of restating this one. */
val VayouListItemPadding: PaddingValues
    @Composable
    @ReadOnlyComposable
    get() = PaddingValues(horizontal = VayouListItemInset, vertical = VayouTheme.spacing.lg)
