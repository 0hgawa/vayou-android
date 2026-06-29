package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouTabRow(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabWidths = remember(tabs.size) { IntArray(tabs.size) }
    var indicatorTarget by remember { mutableIntStateOf(0) }
    val interactionSources = remember(tabs.size) { List(tabs.size) { MutableInteractionSource() } }

    val density = LocalDensity.current
    val pillWidthPx = remember(density) { with(density) { 36.dp.toPx() }.toInt() }

    // Recalculate indicator when selectedIndex changes
    fun recalcIndicator() {
        val tabX = (0 until selectedIndex).sumOf { tabWidths.getOrElse(it) { 0 } }
        val tabW = tabWidths.getOrElse(selectedIndex) { 0 }
        indicatorTarget = tabX + (tabW - pillWidthPx) / 2
    }

    LaunchedEffect(selectedIndex) { recalcIndicator() }

    // Pill centered under the selected tab
    val indicatorXState = animateIntAsState(
        targetValue = indicatorTarget,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "tabX",
    )

    val accent = VayouTheme.colors.accent
    val divider = VayouTheme.colors.outlineVariant.copy(alpha = 0.4f)
    val pillHeightPx = remember(density) { with(density) { 3.dp.toPx() } }
    val dividerHeightPx = remember(density) { with(density) { 1.dp.toPx() } }
    val pillRadiusPx = pillHeightPx / 2f  // capsule

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                // Full-width divider
                drawRect(
                    color = divider,
                    topLeft = Offset(0f, size.height - dividerHeightPx),
                    size = Size(size.width, dividerHeightPx),
                )
                // Pill indicator centered under the selected tab
                val x = indicatorXState.value.toFloat()
                if (tabWidths.any { w -> w > 0 }) {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(x, size.height - pillHeightPx),
                        size = Size(pillWidthPx.toFloat(), pillHeightPx),
                        cornerRadius = CornerRadius(pillRadiusPx),
                    )
                }
            },
    ) {
        Row {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) VayouTheme.colors.onSurface else VayouTheme.colors.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "tabColor",
                )
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .onSizeChanged {
                            tabWidths[index] = it.width
                            recalcIndicator()
                        }
                        .clickable(
                            interactionSource = interactionSources[index],
                            indication = null,
                            onClick = { onTabSelected(index) },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = VayouTheme.typography.labelLarge.copy(fontSize = 15.sp),
                        color = textColor,
                    )
                }
            }
        }
    }
}
