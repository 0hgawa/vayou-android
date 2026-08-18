package dev.vayou.feature.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

/** What a drag turned out to be about. Decided once, on the first movement that clears the slop. */
enum class GestureAxis { Brightness, Volume, Seek }

/**
 * Brightness on the left half, volume on the right, seeking sideways, and tapping on top of all of
 * them.
 *
 * One pointer handler for everything rather than several that fight: the axis is chosen from the
 * first movement and held for the rest of the drag, so a wandering thumb does not start changing
 * the volume halfway through a seek, and a tap is only a tap once the finger has left without
 * travelling.
 *
 * This sits under the controls and hears every touch they hear, so it watches for the ones they
 * have already answered. Without that, pressing a button would both work the button and count as a
 * touch on the film -- the controls would hide themselves as you used them, and two quick presses
 * of the skip button would land as a press and a double tap, jumping twice as far as it says.
 */
fun Modifier.playerGestures(
    /**
     * False while the screen is locked, when every touch is taken to be an accident and answered
     * only by [onTap] — which is how the viewer asks for the way out.
     */
    enabled: Boolean,
    /**
     * Which of the three drags the viewer has left switched on. A drag that would have been one of
     * the others is simply not claimed, so it is free to become one of the ones that are.
     */
    isSeekEnabled: Boolean,
    isBrightnessEnabled: Boolean,
    isVolumeEnabled: Boolean,
    isZoomEnabled: Boolean,
    onTap: () -> Unit,
    /** Where across the picture the second tap landed, from 0 at the leading edge to 1 at the far
     *  one. What that means is the caller's to decide -- it is a preference. */
    onDoubleTap: (horizontalFraction: Float) -> Unit,
    onStart: (GestureAxis) -> Unit,
    onDrag: (GestureAxis, delta: Float) -> Unit,
    onEnd: () -> Unit,
    onPinchStart: () -> Unit,
    onPinch: (zoomChange: Float) -> Unit,
    onPinchEnd: () -> Unit,
    // Keyed on what it reads and not on Unit: the handler runs in a coroutine started once, so a
    // plain parameter read inside it would still be the value it had when the screen first appeared.
): Modifier = pointerInput(enabled, isSeekEnabled, isBrightnessEnabled, isVolumeEnabled, isZoomEnabled) {
    val slop = viewConfiguration.touchSlop
    val doubleTapWindowMs = viewConfiguration.doubleTapTimeoutMillis

    awaitEachGesture {
        // Read per gesture, not once at the top: rotating the phone changes it, and a stale half
        // would put the brightness side of the screen where the volume side now is.
        val halfWidth = size.width / 2f
        val down = awaitFirstDown(requireUnconsumed = false)
        var axis: GestureAxis? = null
        var travelled = Offset.Zero
        var wasTap = false
        var pinching = false

        // In a finally, so the readout cannot be left on screen. A drag that grows a second finger
        // becomes a pinch, and the system can take a gesture away mid-way; on either path the code
        // below the loop never runs, and whatever the drag put up would stay up for good.
        try {
            while (true) {
                val event = awaitPointerEvent()
                val fingers = event.changes.count { it.pressed }

                if (fingers == 0) {
                    val up = event.changes.firstOrNull { it.id == down.id }
                    wasTap = !pinching && axis == null && up != null && !up.isConsumed
                    break
                }

                // A second finger means the framing, never the brightness. Taken for the rest of
                // the gesture: lifting back to one finger while still pinching would otherwise hand
                // the film to whichever axis the remaining thumb happened to be moving along.
                if (enabled && isZoomEnabled && fingers > 1) {
                    if (!pinching) {
                        pinching = true
                        onPinchStart()
                    }
                    onPinch(event.calculateZoom())
                    event.changes.forEach { it.consume() }
                    continue
                }
                if (pinching) continue

                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if (!change.pressed) {
                    wasTap = axis == null && !change.isConsumed
                    break
                }

                // Claimed by something above -- the seek bar being dragged, most likely. Whatever
                // it is, it asked for this gesture first.
                if (axis == null && change.isConsumed) break
                if (!enabled) continue

                travelled += change.positionChange()
                if (axis == null) {
                    if (travelled.getDistance() < slop) continue
                    axis = when {
                        abs(travelled.x) > abs(travelled.y) -> GestureAxis.Seek.takeIf { isSeekEnabled }
                        down.position.x < halfWidth -> GestureAxis.Brightness.takeIf { isBrightnessEnabled }
                        else -> GestureAxis.Volume.takeIf { isVolumeEnabled }
                    }
                    // Switched off. Left unclaimed rather than swallowed, so a thumb that started
                    // sideways can still turn into one of the drags that is on.
                    if (axis == null) continue
                    onStart(axis)
                }

                val delta = change.positionChange()
                when (axis) {
                    // Up is more, the way every phone's own volume gesture already runs.
                    GestureAxis.Brightness, GestureAxis.Volume -> onDrag(axis, -delta.y / size.height)
                    GestureAxis.Seek -> onDrag(axis, delta.x / size.width)
                    null -> Unit
                }
                change.consume()
            }
        } finally {
            if (axis != null) onEnd()
            if (pinching) onPinchEnd()
        }

        if (pinching || axis != null) return@awaitEachGesture
        if (!wasTap) return@awaitEachGesture

        // The finger went down and came back up without travelling, and nothing else wanted it, so
        // it was a tap on the film. Answer it now instead of waiting out the double-tap window: a
        // third of a second before the controls move reads as the app hesitating, and a second tap
        // can undo this one for nothing.
        onTap()
        if (!enabled) return@awaitEachGesture
        val second = withTimeoutOrNull(doubleTapWindowMs) { awaitFirstDown(requireUnconsumed = true) }
        if (second != null) onDoubleTap(second.position.x / size.width)
    }
}
