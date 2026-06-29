package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = VayouTheme.colors.divider,
) {
    val dividerColor = color
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .drawBehind { drawRect(dividerColor) },
    )
}

@Composable
fun VayouVerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = VayouTheme.colors.divider,
) {
    val dividerColor = color
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness)
            .drawBehind { drawRect(dividerColor) },
    )
}
