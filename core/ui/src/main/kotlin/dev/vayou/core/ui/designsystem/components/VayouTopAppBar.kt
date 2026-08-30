package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VayouTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = VayouTheme.colors.surface,
        // The same colour once the list is scrolled under it. Material tints a scrolled bar with an
        // elevated grey by default, to part it from the content passing beneath; this app parts
        // nothing by elevation anywhere else, and all five of its bars are the flat surface in every
        // state. The collapse already announces itself by the title rising into the bar.
        scrolledContainerColor = VayouTheme.colors.surface,
        navigationIconContentColor = VayouTheme.colors.onSurface,
        actionIconContentColor = VayouTheme.colors.onSurface,
        titleContentColor = VayouTheme.colors.onSurface,
    ),
) {
    // On the leading edge, always. A title used to be centred on the pages you arrive at and left at
    // the root of a tab, on the theory that a name beside a back arrow is the name of what you are
    // inside. In practice it made the same string move as you walked in: "Video" on the margin, then
    // the folder's name in the middle of the bar, then a settings page in the middle again. One
    // column for every title, the same one every row beneath it starts on, and nothing jumps.
    TopAppBar(
        // Set rather than inherited: this theme hands Material only its colours, so a title left
        // alone would come back in Material's own titleLarge -- the same size, in the regular
        // weight, beside a bar whose title is semibold.
        title = { ProvideTextStyle(VayouTheme.typography.titleLarge, title) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VayouTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = VayouTheme.colors.surface,
        // The same colour once the list is scrolled under it. Material tints a scrolled bar with an
        // elevated grey by default, to part it from the content passing beneath; this app parts
        // nothing by elevation anywhere else, and all five of its bars are the flat surface in every
        // state. The collapse already announces itself by the title rising into the bar.
        scrolledContainerColor = VayouTheme.colors.surface,
        navigationIconContentColor = VayouTheme.colors.onSurface,
        actionIconContentColor = VayouTheme.colors.onSurface,
        titleContentColor = VayouTheme.colors.onSurface,
    ),
    /**
     * False while something below the bar is showing the same name -- the cover of an open group, the
     * first line of an open folder. The bar takes it over as that scrolls away.
     *
     * Faded rather than swapped in: the name has to look like it was handed over, and a title that
     * simply appears at one frame reads as a second, different title.
     */
    titleVisible: Boolean = true,
) {
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        label = "topBarTitle",
    )
    VayouTopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.alpha(titleAlpha),
                style = VayouTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        modifier = modifier,
    )
}
