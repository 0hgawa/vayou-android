package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/**
 * One setting: what it is, what it does, and where it stands.
 *
 * The same row the rest of the app lists things with, so a screen of settings reads like a screen
 * of anything else rather than like a control panel.
 */
@Composable
fun PreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
            }
        },
        content = { Text(text = title) },
        supportingContent = description?.let { { Text(text = it) } },
        trailingContent = trailingContent,
    )
}

/** A setting that is on or off. The whole row toggles it, not just the switch. */
@Composable
fun PreferenceSwitch(
    title: String,
    isChecked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    PreferenceItem(
        title = title,
        modifier = modifier,
        description = description,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        // Null, not a no-op: the row carries the tap, and a switch that also took one would fight
        // the row for the same gesture.
        trailingContent = { VayouSwitch(checked = isChecked, onCheckedChange = null, enabled = enabled) },
    )
}

/** A setting whose value is one of several, shown on the row and picked in a sheet. */
@Composable
fun PreferenceChoice(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    PreferenceItem(
        title = title,
        modifier = modifier,
        description = value,
        icon = icon,
        onClick = onClick,
        trailingContent = {
            Icon(
                imageVector = VayouIcons.ChevronRight,
                contentDescription = null,
                tint = VayouTheme.colors.onSurfaceVariant,
            )
        },
    )
}

/**
 * A setting that is on or off, where the row leads somewhere as well.
 *
 * The two are parted by a rule, because they do two different things: the switch turns the setting
 * on, the rest of the row opens what it defers to. Without the rule the row is one target that
 * happens to behave differently at one end.
 */
@Composable
fun PreferenceSwitchWithDivider(
    title: String,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    PreferenceItem(
        title = title,
        modifier = modifier,
        description = description,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VerticalDivider(
                    modifier = Modifier
                        .padding(end = DividerGap)
                        .height(DividerHeight),
                    color = VayouTheme.colors.onSurface.copy(alpha = DividerAlpha),
                )
                VayouSwitch(checked = isChecked, onCheckedChange = { onCheckedChange() }, enabled = enabled)
            }
        },
    )
}

/** One of a set, where exactly one is in force. The radio only reports; the row carries the tap. */
@Composable
fun SingleSelectablePreference(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    /** Given by a sheet, which has a narrower margin than the settings screen this defaults to. */
    contentPadding: PaddingValues = VayouListItemPadding,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        contentPadding = contentPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        leadingContent = { VayouRadio(selected = selected, onClick = null) },
        content = { Text(text = title, maxLines = 1) },
        supportingContent = description?.let { { Text(text = it, overflow = TextOverflow.Ellipsis) } },
    )
}

/**
 * One of a set, where any number may be in force -- and where being in force means being left out.
 *
 * Struck through rather than merely ticked: the tick says the box is on, the strike says what that
 * costs, and on a screen of folders to exclude the second is the thing being read.
 */
@Composable
fun SelectablePreference(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val decoration = if (selected) TextDecoration.LineThrough else TextDecoration.None
    VayouSegmentedListItem(
        modifier = modifier,
        onClick = onClick,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        content = {
            Text(text = title, maxLines = 1, textDecoration = decoration)
        },
        supportingContent = description?.let {
            { Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = decoration) }
        },
        trailingContent = {
            VayouCheckbox(
                modifier = Modifier.semantics { contentDescription = title },
                checked = selected,
                onCheckedChange = null,
            )
        },
    )
}

/** A setting on a scale, with the slider under the title rather than beside it. */
@Composable
fun PreferenceSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
) {
    VayouSegmentedListItem(
        modifier = modifier,
        enabled = enabled,
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
            }
        },
        content = { Text(text = title) },
        supportingContent = {
            Column {
                if (description != null) Text(text = description)
                VayouSlider(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    value = value,
                    valueRange = valueRange,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                )
            }
        },
        trailingContent = trailingContent,
    )
}

private val DividerGap = 12.dp

private val DividerHeight = 40.dp

private const val DividerAlpha = 0.3f
