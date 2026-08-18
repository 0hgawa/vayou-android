package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The bar that takes over while things are being marked.
 *
 * It replaces the ordinary bar rather than sitting under it: everything up there belongs to browsing
 * -- the sort, the search, the cast key -- and none of it means anything while a selection is being
 * built. Two bars would also cost the list a row of height for no reading.
 *
 * The count and the way out are one pill. Leaving a selection is the commonest thing done from here,
 * and a cross alone beside a number reads as "clear the number" rather than "stop".
 */
@Composable
fun VayouSelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    VayouTopAppBar(
        modifier = modifier,
        title = "",
        navigationIcon = {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(VayouTheme.colors.surfaceContainerHigh)
                    .clickable(onClick = onExit)
                    .padding(PillPadding)
                    // Extra on the end, so the count is not pressed against the pill's curve.
                    .padding(end = PillPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PillPadding),
            ) {
                Icon(imageVector = VayouIcons.Close, contentDescription = stringResource(R.string.stop_selecting))
                Text(
                    text = stringResource(R.string.n_of_m_selected, selectedCount, totalCount),
                    style = VayouTheme.typography.labelLarge,
                )
            }
        },
        actions = actions,
    )
}

private val PillPadding = 8.dp
