package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Something is happening and nobody knows how long it will take.
 *
 * An arc that both turns and breathes: the sweep grows and shrinks against a steady rotation, so the
 * mark never settles into a shape the eye stops reading as motion.
 */
@Composable
fun VayouCircularProgress(
    modifier: Modifier = Modifier,
    color: Color = VayouTheme.colors.accent,
    strokeWidth: Dp = 3.dp,
    size: Dp = 40.dp,
) {
    val transition = rememberInfiniteTransition(label = "progress")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(RotationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )

    val sweep by transition.animateFloat(
        initialValue = MinSweep,
        targetValue = MaxSweep,
        animationSpec = infiniteRepeatable(tween(SweepMs), RepeatMode.Reverse),
        label = "sweep",
    )

    Canvas(modifier = modifier.size(size)) {
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * A screen with nothing on it yet, because what goes there has not arrived.
 *
 * The counterpart to [VayouEmptyState], and the distinction between them is the whole point: one
 * says there is nothing, the other says not yet. Three copies of it were drawn by hand across
 * the music and network screens before this existed.
 */
@Composable
fun VayouWaiting(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        VayouCircularProgress()
    }
}

private const val RotationMs = 1_100

private const val SweepMs = 800

private const val MinSweep = 30f

private const val MaxSweep = 270f
