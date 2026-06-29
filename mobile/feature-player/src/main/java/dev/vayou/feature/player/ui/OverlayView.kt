package dev.vayou.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.feature.player.extensions.noRippleClickable

internal val OverlayContentPadding = 24.dp

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    maxHeightFraction: Float = 0.45f,
    onBack: (() -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val endPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateEndPadding(layoutDirection)

    AnimatedVisibility(
        modifier = Modifier.align(
            if (configuration.isPortrait) {
                Alignment.BottomCenter
            } else {
                Alignment.CenterEnd
            },
        ),
        visible = show,
        enter = if (configuration.isPortrait) slideInVertically { it } else slideInHorizontally { it },
        exit = if (configuration.isPortrait) slideOutVertically { it } else slideOutHorizontally { it },
    ) {
        val shape = if (configuration.isPortrait) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        }
        // Sheet uses heightIn(max=…) instead of fillMaxHeight(fraction) so it
        // wraps the content vertically (a two-item audio track list now ends
        // right under the last row instead of leaving 40% of the screen empty)
        // while still capping at maxHeightFraction of the viewport for long
        // content. Same idea on landscape with widthIn(max=…).
        val maxSheetHeight = (configuration.screenHeightDp * maxHeightFraction).dp
        val maxSheetWidth = (configuration.screenWidthDp * maxHeightFraction).dp
        Box(
            modifier = modifier
                .imePadding()
                .then(
                    if (configuration.isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight)
                    } else {
                        Modifier
                            .widthIn(max = maxSheetWidth)
                            .fillMaxHeight()
                    },
                )
                .clip(shape)
                .background(VayouTheme.colors.surfaceContainer)
                .noRippleClickable {},
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(end = endPadding),
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = if (onBack != null) 8.dp else OverlayContentPadding,
                        end = if (trailingAction != null) 8.dp else OverlayContentPadding,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        VayouIconButton(onClick = onBack) {
                            Icon(
                                imageVector = VayouIcons.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = VayouTheme.typography.headlineSmall,
                        color = VayouTheme.colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (trailingAction != null) {
                        trailingAction()
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                content()
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOverlayView() {
    VayouPlayerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            OverlayView(modifier = Modifier.align(Alignment.BottomCenter), title = "Selector view", show = true) {
                Text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum")
            }
        }
    }
}
