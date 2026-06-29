package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val PositionalThreshold = 80.dp
private val IndicatorSize = 40.dp
private const val DragMultiplier = 0.5f

@Composable
fun VayouPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { PositionalThreshold.toPx() }
    val indicatorPx = with(density) { IndicatorSize.toPx() }
    val scope = rememberCoroutineScope()
    val anim = remember { Animatable(0f) }
    val state = remember(thresholdPx) {
        VayouPullToRefreshState(
            anim = anim,
            scope = scope,
            thresholdPx = thresholdPx,
            isRefreshingProvider = { _isRefreshing },
            onRefresh = { _onRefresh() },
        )
    }

    // Hoist callbacks/flags into the state without recreating it on every recomposition.
    state._isRefreshing = isRefreshing
    state._onRefresh = onRefresh

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            anim.animateTo(thresholdPx, tween(250))
        } else {
            anim.animateTo(0f, tween(250))
        }
    }

    Box(modifier = modifier.nestedScroll(state.nestedScrollConnection)) {
        content()

        if (anim.value > 0f || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (anim.value - indicatorPx).roundToInt().coerceAtLeast(0)) }
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                VayouCircularProgress()
            }
        }
    }
}

private class VayouPullToRefreshState(
    private val anim: Animatable<Float, *>,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val thresholdPx: Float,
    private val isRefreshingProvider: VayouPullToRefreshState.() -> Boolean,
    private val onRefresh: VayouPullToRefreshState.() -> Unit,
) {
    var _isRefreshing: Boolean = false
    var _onRefresh: () -> Unit = {}

    private var distancePulled: Float = 0f

    private val adjustedDistance: Float
        get() = distancePulled * DragMultiplier

    private fun calculateOffset(): Float {
        val adjusted = adjustedDistance
        if (adjusted <= thresholdPx) return adjusted
        val overshoot = (adjusted / thresholdPx) - 1f
        val linearTension = overshoot.coerceIn(0f, 2f)
        val tensionPercent = linearTension - linearTension.pow(2) / 4f
        return thresholdPx + thresholdPx * tensionPercent
    }

    private fun consume(available: Offset): Offset {
        if (_isRefreshing) return Offset.Zero
        val newDistance = (distancePulled + available.y).coerceAtLeast(0f)
        val consumed = newDistance - distancePulled
        if (consumed == 0f) return Offset.Zero
        distancePulled = newDistance
        scope.launch { anim.snapTo(calculateOffset()) }
        return Offset(0f, consumed)
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            return when {
                anim.isRunning -> Offset.Zero
                source == NestedScrollSource.UserInput && available.y < 0 -> consume(available)
                else -> Offset.Zero
            }
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            return when {
                anim.isRunning -> Offset.Zero
                source == NestedScrollSource.UserInput -> consume(available)
                else -> Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (_isRefreshing) {
                distancePulled = 0f
                return Velocity.Zero
            }
            if (distancePulled == 0f) return Velocity.Zero
            val shouldRefresh = adjustedDistance > thresholdPx
            if (shouldRefresh) {
                _onRefresh()
            }
            val consumed = if (available.y < 0f) 0f else available.y
            distancePulled = 0f
            if (!shouldRefresh && anim.value > 0f) {
                anim.animateTo(0f, tween(250))
            }
            return Velocity(0f, consumed)
        }
    }
}
