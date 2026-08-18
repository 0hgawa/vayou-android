package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How far through something playback is, and the way to change it.
 *
 * One component for both players. They had one each -- a `Canvas` on the music screen and a stack
 * of boxes on the film screen -- and the two disagreed about where the handle sits, which is
 * visible the moment anyone opens both. Two drawings of one control drift by definition; this is
 * the drawing.
 *
 * Colours are the caller's because the two backgrounds are not comparable: the music screen sits on
 * a surface from the palette, the film screen on a frame of video whose brightness nobody chose and
 * which therefore needs fixed white.
 */
@Composable
fun VayouSeekBar(
    /** 0f..1f. The caller owns the arithmetic, since one screen counts in millis and the other does not. */
    fraction: Float,
    onSeekTo: (Float) -> Unit,
    trackColor: Color,
    activeColor: Color,
    thumbColor: Color,
    modifier: Modifier = Modifier,
    /**
     * A drag has begun on the bar, or the gesture has ended.
     *
     * True only once the finger has actually moved: the film player turns this into keyframe seeking
     * for as long as it lasts, and a tap is not a drag. False always, movement or not -- the music
     * screen holds its seek back until the gesture is over and this is what tells it.
     */
    onScrub: (Boolean) -> Unit = {},
    /** Where a repeat begins and ends, as fractions. Drawn so the stretch can be seen, not guessed. */
    marks: List<Float> = emptyList(),
    markColor: Color = activeColor,
    height: Dp = DefaultHeight,
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    // While a finger is down the bar follows the finger, not the player: the player answers a seek
    // a moment later, and letting it win in the meantime would drag the handle back under the thumb.
    var draggedFraction by remember { mutableFloatStateOf(UnsetFraction) }
    val shown = if (draggedFraction != UnsetFraction) draggedFraction else fraction.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // One gesture, claimed from the first touch.
            //
            // Not Compose's tap and drag detectors: both wait for a slop distance before they
            // consume anything, and in that gap whatever lies behind the bar sees an unclaimed
            // finger moving sideways and starts a gesture of its own. On the film screen that was
            // the seek gesture -- two seeks on one drag, by two different rules.
            //
            // Claiming the down also makes a tap a seek to that point rather than a tap on what is
            // behind, which is what a progress bar is for.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    var at = (down.position.x / widthPx).coerceIn(0f, 1f)
                    draggedFraction = at
                    onSeekTo(at)

                    var isDragging = false
                    var sentAt = down.uptimeMillis
                    var sentFraction = at

                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        // Only now is this a drag. Entering keyframe mode on the way down would put
                        // a player into it and straight back out again for every tap on the bar.
                        if (!isDragging) {
                            isDragging = true
                            onScrub(true)
                        }
                        at = (change.position.x / widthPx).coerceIn(0f, 1f)
                        draggedFraction = at

                        // Seeking as the finger moves, not only when it lifts: a bar that travels
                        // over a picture that does not is a scrollbar, and finding a scene would
                        // mean letting go and looking, over and over.
                        //
                        // But not on every event. A finger produces these as fast as the screen
                        // reports, and no decoder shows a hundred and twenty positions a second --
                        // each one is a seek across the session for a frame nobody sees. The handle
                        // does not wait for any of it, because it is drawn from the finger.
                        if (change.uptimeMillis - sentAt >= SeekIntervalMs) {
                            sentAt = change.uptimeMillis
                            sentFraction = at
                            onSeekTo(at)
                        }
                    }

                    draggedFraction = UnsetFraction
                    // Where the finger actually left, whatever the throttle above swallowed.
                    if (at != sentFraction) onSeekTo(at)
                    onScrub(false)
                }
            },
    ) {
        widthPx = size.width

        val trackHeight = TrackHeight.toPx()
        val centreY = size.height / 2f
        val trackY = centreY - trackHeight / 2f
        val corner = CornerRadius(trackHeight / 2f)

        drawRoundRect(trackColor, Offset(0f, trackY), Size(size.width, trackHeight), corner)

        val activeWidth = size.width * shown
        if (activeWidth > 0f) {
            drawRoundRect(activeColor, Offset(0f, trackY), Size(activeWidth, trackHeight), corner)
        }

        // Under the handle, so a mark the handle is sitting on does not draw over it.
        val markWidth = MarkWidth.toPx()
        val markHeight = MarkHeight.toPx()
        marks.forEach { mark ->
            val x = (size.width * mark.coerceIn(0f, 1f)).coerceAtMost(size.width - markWidth)
            drawRoundRect(
                color = markColor,
                topLeft = Offset(x, centreY - markHeight / 2f),
                size = Size(markWidth, markHeight),
                cornerRadius = CornerRadius(markWidth / 2f),
            )
        }

        // Centred on the end of what has played, not inset by its own radius. Half of it hangs off
        // the bar at each extreme, and that is the right answer: the handle marks a position, and a
        // position at nought is at nought. Inset instead, it stops short of the end it is reporting.
        drawCircle(thumbColor, ThumbRadius.toPx(), Offset(activeWidth, centreY))
    }
}

/** No finger down. Not zero, which is a real position at the start. */
private const val UnsetFraction = -1f

/**
 * How often a drag is allowed to seek.
 *
 * Fifty milliseconds: twenty a second, which is faster than a decoder settles on a new position and
 * far slower than a finger reports. Below this the extra seeks are cancelled by the next one before
 * anything is drawn from them.
 */
private const val SeekIntervalMs = 50L

/** The line the bar is drawn on: 4dp of track, and the rest is what a thumb has to land on. */
private val DefaultHeight = 40.dp

private val TrackHeight = 4.dp

private val ThumbRadius = 8.dp

/** Taller than the track, so a mark reads as a pin through it rather than a gap in it. */
private val MarkHeight = 12.dp

private val MarkWidth = 3.dp
