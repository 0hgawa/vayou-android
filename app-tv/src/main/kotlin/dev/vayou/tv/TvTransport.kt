package dev.vayou.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A round button that fills with white when focus lands on it.
 *
 * Filled rather than scaled, unlike the cards on the home screen: these sit shoulder to shoulder in
 * a row, and one growing over its neighbours reads as a wobble rather than as a selection. Every one
 * is the same size, as a television's own player draws them: the row is walked with a D-pad, so the
 * fill is what says "you are here" and a bigger button says nothing a viewer needs.
 *
 * [isGrouped] leaves the resting fill off, for a button sharing a capsule with its neighbour: the
 * capsule already draws it, and a second one inside would read as a bubble within a bubble.
 */
@Composable
fun TvControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGrouped: Boolean = false,
) {
    TvControlButton(label = label, onClick = onClick, modifier = modifier, isGrouped = isGrouped) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(VayouTheme.iconSize.md))
    }
}

/**
 * The same button saying a word instead of drawing a glyph.
 *
 * The speed is the one control whose value is shorter than any picture of it: "1.5x" says what a
 * dial can only hint at, and the phone's button has always shown the number rather than a mark.
 */
@Composable
fun TvControlButton(
    text: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGrouped: Boolean = false,
) {
    TvControlButton(label = label, onClick = onClick, modifier = modifier, isGrouped = isGrouped) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

@Composable
private fun TvControlButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    isGrouped: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(TvButtonSize),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isGrouped) Color.Transparent else TvRestingFill,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { content() }
    }
}

/**
 * Buttons that answer one question, in one bubble.
 *
 * The way a television's own player groups them: two halves of "which item", or the four that say
 * how this one plays. A bubble each would read as that many more buttons in a row that already has
 * six, and the focus fills only the half it is on.
 */
@Composable
fun TvControlCapsule(content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.background(TvRestingFill, CircleShape), content = content)
}

/**
 * The line, and the only control that a press moves rather than triggers.
 *
 * It shows where a viewer is heading while they hold the key down and only seeks when they let go:
 * asking the player to jump on every repeat is a hundred seeks for one scrub. The step grows the
 * longer the key is held, or crossing an hour would take a hundred presses.
 */
@Composable
fun TvSeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    var previewMs by remember { mutableLongStateOf(positionMs) }
    // Counted here rather than read off the event: how far one press moves depends on how long the
    // key has been down, and this is the same number without reaching for the platform's.
    var pressesHeld by remember { mutableIntStateOf(0) }
    LaunchedEffect(positionMs, isFocused) { if (!isFocused) previewMs = positionMs }

    val shownMs = if (isFocused) previewMs else positionMs
    val played = if (durationMs > 0L) (shownMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val trackHeight by animateDpAsState(
        targetValue = if (isFocused) FocusedTrack else RestingTrack,
        animationSpec = tween(TrackGrowMs),
        label = "seek-track",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SeekHeight)
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (durationMs <= 0L) return@onPreviewKeyEvent false
                val step = when (event.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> return@onPreviewKeyEvent false
                }
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        previewMs = (previewMs + step * stepFor(pressesHeld)).coerceIn(0L, durationMs)
                        pressesHeld++
                        true
                    }

                    KeyEventType.KeyUp -> {
                        onSeek(previewMs)
                        pressesHeld = 0
                        true
                    }

                    else -> false
                }
            },
    ) {
        val width = maxWidth
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = TrackAlpha)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(played)
                .height(trackHeight)
                .clip(CircleShape)
                // White, like the thumb and the buttons. Over a film the amber was the only thing
                // on screen wearing the app's own colour, and a line of it across the picture reads
                // as a mark on the film rather than as how far through it you are.
                .background(Color.White),
        )
        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = width * played - Thumb / 2)
                    .size(Thumb)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

/** Five seconds a press, up to a minute once the key has been held for a while. */
private fun stepFor(pressesHeld: Int): Long = when {
    pressesHeld < 2 -> 5_000L
    pressesHeld < 6 -> 15_000L
    pressesHeld < 12 -> 30_000L
    else -> 60_000L
}

/** Hours only where there are hours: a three-minute track has no business reading 0:03:12. */
fun tvClock(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val seconds = ms / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds % 60)
    } else {
        "%02d:%02d".format(minutes, seconds % 60)
    }
}

/** What an unpressed button sits on: a hint of the panel rather than a plate. */
val TvRestingFill = Color.White.copy(alpha = 0.08f)

val TvButtonSize = 48.dp

/** How often a running clock is read. Finer than the eye reads a bar this wide, and no finer. */
const val TvTickMs = 500L

private const val TrackAlpha = 0.2f

private const val TrackGrowMs = 150

private val SeekHeight = 20.dp

private val RestingTrack = 4.dp

private val FocusedTrack = 6.dp

private val Thumb = 16.dp
