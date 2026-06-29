package dev.vayou.core.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
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
    val contentColor = contentColorOverride ?: when {
        selected -> VayouTheme.colors.onAccentContainer
        enabled -> VayouTheme.colors.onSurface
        else -> VayouTheme.colors.onDisabled
    }
    val subColor = supportingColorOverride ?: when {
        selected -> VayouTheme.colors.onAccentContainer
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
    color: Color = VayouTheme.colors.accentOnBackground,
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
