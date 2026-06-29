package dev.vayou.feature.player.ui

import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.theme.VayouPlayerTheme

private const val NORMAL_MAX_PERCENTAGE = 100

/**
 * Vertical progress indicator for volume/brightness.
 *
 * @param value Current value (0 to maxValue)
 * @param maxValue Maximum value (100 for normal, 200 for boosted volume)
 * @param icon Icon to display at the bottom
 */
@Composable
fun VerticalProgressView(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    icon: Painter,
    @IntRange(from = 0, to = 200) value: Int,
    maxValue: Int = NORMAL_MAX_PERCENTAGE,
) {
    val normalizedValue = value.coerceIn(0, maxValue)
    val fillFraction = normalizedValue.toFloat() / maxValue.toFloat()
    val isBoostActive = maxValue > NORMAL_MAX_PERCENTAGE && value > NORMAL_MAX_PERCENTAGE

    Column(
        modifier = modifier
            .heightIn(max = 250.dp)
            .clip(CircleShape)
            .background(VayouTheme.colors.background)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.size(width),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = normalizedValue.toString(),
                style = VayouTheme.typography.labelLarge.copy(
                    color = VayouTheme.colors.onBackground,
                ),
                autoSize = TextAutoSize.StepBased(maxFontSize = VayouTheme.typography.labelLarge.fontSize),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .width(width)
                .clip(VayouTheme.shapes.medium)
                .background(VayouTheme.colors.surfaceVariant),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight(fillFraction)
                    .background(if (isBoostActive) VayouTheme.colors.error else VayouTheme.colors.accent),
            )
        }
        Box(
            modifier = Modifier.size(width),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = VayouTheme.colors.onBackground,
            )
        }
    }
}

@Preview
@Composable
private fun VerticalProgressPreview() {
    VayouPlayerTheme {
        VerticalProgressView(
            value = 50,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}

@Preview
@Composable
private fun VerticalProgressBoostPreview() {
    VayouPlayerTheme {
        VerticalProgressView(
            value = 150,
            maxValue = 200,
            icon = painterResource(R.drawable.ic_volume),
        )
    }
}
