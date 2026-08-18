package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberZoomState(): ZoomState = remember { ZoomState() }

/**
 * How far into the picture the viewer has pinched.
 *
 * Separate from the framing beside it: framing decides how the film meets the edges of the screen,
 * this decides how much of the film you are looking at. Choosing a framing puts this back to one,
 * because the two would otherwise compound into a size nobody asked for.
 *
 * Zoom only, and about the centre. The old player could be dragged around at zoom as well, behind a
 * preference that ships off, and there is a reason it ships off: a pinch reports the movement of the
 * midpoint between the two fingers, and no real pair of thumbs moves symmetrically, so every pinch
 * carries a drift the viewer did not ask for. The picture creeps up the screen while growing.
 */
@Stable
class ZoomState {
    var zoom: Float by mutableFloatStateOf(NoZoom)
        private set

    fun reset() {
        zoom = NoZoom
    }

    fun restore(saved: Float) {
        zoom = saved.coerceIn(MinZoom, MaxZoom)
    }

    fun pinch(zoomChange: Float) {
        zoom = (zoom * zoomChange).coerceIn(MinZoom, MaxZoom)
    }
}

private const val NoZoom = 1f

/** Small enough to see a whole ultrawide frame letterboxed, large enough to read a caption burnt in. */
private const val MinZoom = 0.25f

private const val MaxZoom = 4f
