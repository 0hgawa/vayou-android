package dev.vayou.core.ui.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.ui.designsystem.theme.VayouTheme

object MediaListLayoutDefaults {

    @Composable
    @ReadOnlyComposable
    fun outerHorizontal(mode: MediaLayoutMode): Dp =
        if (mode == MediaLayoutMode.GRID) VayouTheme.spacing.sm else 0.dp

    val GridItemPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(VayouTheme.spacing.sm)

    val ListItemPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(horizontal = VayouTheme.spacing.lg, vertical = VayouTheme.spacing.sm)

    @Composable
    @ReadOnlyComposable
    fun freeSpanHorizontal(mode: MediaLayoutMode): Dp =
        if (mode == MediaLayoutMode.GRID) VayouTheme.spacing.sm else VayouTheme.spacing.lg

    val GridItemSpacing: Dp
        @Composable
        @ReadOnlyComposable
        get() = VayouTheme.spacing.xxs

    val ListItemSpacing: Dp = 0.dp

    @Composable
    @ReadOnlyComposable
    fun itemSpacing(mode: MediaLayoutMode): Dp =
        if (mode == MediaLayoutMode.GRID) GridItemSpacing else ListItemSpacing
}
