package dev.vayou.core.ui.designsystem.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import dev.vayou.core.ui.designsystem.theme.VayouTheme

private const val DismissThreshold = 120f

@Composable
fun VayouBottomSheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = VayouTheme.typography.titleMedium,
        color = VayouTheme.colors.onSurface,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun VayouBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val dismiss: () -> Unit = { visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(220)
            onDismissRequest()
        }
    }

    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Popup(
        onDismissRequest = dismiss,
        properties = PopupProperties(
            focusable = true,
            clippingEnabled = false,
        ),
    ) {
        BackHandler(onBack = dismiss)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180)),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VayouTheme.colors.scrim.copy(alpha = 0.32f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = dismiss,
                        ),
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { it },
                ),
            ) {
                Column(
                    modifier = modifier
                        .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(VayouTheme.colors.surfaceContainer)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffset.value > DismissThreshold) {
                                        visible = false
                                    } else {
                                        scope.launch { dragOffset.animateTo(0f, spring(0.85f, Spring.StiffnessMediumLow)) }
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (dragOffset.value + dragAmount).coerceAtLeast(0f)
                                    scope.launch { dragOffset.snapTo(newOffset) }
                                },
                            )
                        }
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .width(32.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(VayouTheme.colors.onSurfaceVariant.copy(alpha = 0.4f)),
                    )
                    content()
                }
            }
        }
    }
}
