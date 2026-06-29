package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = VayouTheme.colors.surface,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor),
    ) {
        topBar()
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        bottomBar()
    }
}
