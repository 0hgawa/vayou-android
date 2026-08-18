package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/** One place the app can be. The two icons are the same drawing outlined and filled. */
@Immutable
data class VayouNavBarItem(val icon: ImageVector, val selectedIcon: ImageVector, val label: String)

/**
 * Where the app is, along the bottom.
 *
 * Selection is said by the icon filling in, not by a colour or a pill behind it. Four destinations
 * that are always all four, so the question is only which one, and a filled shape answers it at a
 * glance without a second colour in the palette.
 */
@Composable
fun VayouNavBar(
    items: List<VayouNavBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(VayouTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(BarHeight)
            .selectableGroup(),
    ) {
        items.forEachIndexed { index, item ->
            Item(
                item = item,
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                arrangement = Arrangement.Center,
            )
        }
    }
}

/**
 * The same destinations down the side, for windows at least Medium wide -- what Material asks for
 * there, and on a phone that means landscape, where a bottom bar would eat height that is already
 * scarce.
 *
 * It consumes the leading and vertical safe-drawing insets, so the caller marks the leading side as
 * handled for whatever sits beside it.
 */
@Composable
fun VayouNavRail(
    items: List<VayouNavBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(VayouTheme.colors.surface)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
            )
            .width(RailWidth)
            .selectableGroup(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEachIndexed { index, item ->
            Item(
                item = item,
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
                // Margin first, then the full width: every item is the same width whatever its
                // label says, so the press highlight is identical down the rail. The bar gets that
                // from weight(1f); here the width is fixed and has to be asked for.
                modifier = Modifier
                    .padding(horizontal = RailItemMargin, vertical = RailItemGap)
                    .fillMaxWidth(),
                innerPadding = RailItemPadding,
            )
        }
    }
}

@Composable
private fun Item(
    item: VayouNavBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Vertical = Arrangement.Top,
    innerPadding: Dp = 0.dp,
) {
    Column(
        modifier = modifier
            // Clipped before it is selectable, so the press ripple is bounded by the rounded shape
            // rather than filling the cell as a hard rectangle.
            .clip(VayouTheme.shapes.large)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = arrangement,
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = null,
            tint = VayouTheme.colors.onSurface,
            modifier = Modifier.size(VayouTheme.iconSize.md),
        )
        Spacer(modifier = Modifier.height(LabelGap))
        Text(
            text = item.label,
            style = VayouTheme.typography.labelSmall,
            color = VayouTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val BarHeight = 50.dp

private val RailWidth = 80.dp

private val RailItemMargin = 8.dp

private val RailItemGap = 4.dp

private val RailItemPadding = 8.dp

/** Tight: the label names the icon above it rather than sitting under it as a separate line. */
private val LabelGap = 2.dp
