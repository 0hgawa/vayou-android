package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.vayou.core.ui.theme.VayouTheme

@Composable
fun VayouScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    /** Drawn over the foot of the content, above the bar rather than across it. */
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = VayouTheme.colors.surface,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor),
    ) {
        topBar()
        Box(
            modifier = Modifier
                .weight(1f)
                // Horizontal safe area for the content. In landscape the system bar and any display
                // cutout move to a side, and without this every screen would have to clear them by
                // hand. Only the sides are taken: the top bar owns the status bar inset and the
                // bottom bar owns the navigation bar one.
                //
                // windowInsetsPadding also *consumes* what it applies, so a VayouScaffold nested
                // inside another one — or a Material Scaffold inside this one — sees zero here and
                // does not pad a second time.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            content()
            Box(modifier = Modifier.align(Alignment.BottomCenter)) { snackbarHost() }
        }
        bottomBar()
    }
}
