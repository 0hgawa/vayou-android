package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Three bars that rise and fall: this is the one that is playing.
 *
 * Paused, they stop where they are rather than disappearing -- the row is still the current one,
 * and a mark that vanishes on pause says the queue lost its place.
 */
@Composable
fun VayouPlayingIndicator(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    /**
     * Not the accent: these bars are the only thing saying which row is playing, so they have to
     * hold against the surface rather than sit on it as decoration.
     */
    color: Color = VayouTheme.colors.onSurface,
) {
    if (!isPlaying) {
        PlayingBars(heights = RestingHeights, color = color, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "playingIndicator")
    val heights = BarDurationsMs.mapIndexed { index, durationMs ->
        val height by transition.animateFloat(
            // Alternating, so the three never rise together and read as one block moving.
            initialValue = if (index % 2 == 0) MinHeight else MaxHeight,
            targetValue = if (index % 2 == 0) MaxHeight else MinHeight,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$index",
        )
        height
    }
    PlayingBars(heights = heights, color = color, modifier = modifier)
}

@Composable
private fun PlayingBars(heights: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier = modifier) {
        // Bars and the gaps between them share a width, which keeps the group centred whatever size
        // the caller gives it.
        val barWidth = size.width / (heights.size * 2 - 1)
        val corner = CornerRadius(barWidth / 2f)
        heights.forEachIndexed { index, fraction ->
            val height = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * barWidth * 2f, size.height - height),
                size = Size(barWidth, height),
                cornerRadius = corner,
            )
        }
    }
}

private const val MinHeight = 0.25f

private const val MaxHeight = 1f

/** Deliberately unequal, and none a multiple of another, so the cycle never visibly repeats. */
private val BarDurationsMs = listOf(420, 630, 500)

private val RestingHeights = listOf(0.45f, 0.75f, 0.35f)
