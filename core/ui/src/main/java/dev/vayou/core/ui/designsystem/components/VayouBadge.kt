package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = VayouTheme.colors.accentContainer,
    contentColor: Color = VayouTheme.colors.onAccentContainer,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides VayouTheme.typography.labelSmall,
    ) {
        Box(
            modifier = modifier
                .clip(VayouTheme.shapes.extraSmall)
                .background(containerColor)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
