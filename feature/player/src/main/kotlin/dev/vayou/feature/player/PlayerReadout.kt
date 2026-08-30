package dev.vayou.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vayou.core.player.ui.VideoContentScale
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme
import kotlin.time.Duration.Companion.milliseconds

/** What the pill is saying. */
@Immutable
data class PlayerReadout(val glyph: ImageVector, val text: String)

/**
 * A level being set, while the finger setting it is still down.
 *
 * Volume and brightness only, and at the top. They are levels rather than statements about the
 * picture: they persist after the gesture, they run nought to a hundred, and they are set *while
 * the film is watched* -- so the readout keeps out of the picture, and out of the transport, which
 * can be on screen at the same time. Covering play with a number is not a trade worth making.
 *
 * What the picture itself is doing -- a seek, a skip, a change of framing -- goes to
 * [PlayerCentreReadout] instead.
 */
@Composable
fun PlayerReadoutPill(readout: PlayerReadout?, modifier: Modifier = Modifier) {
    // Held past the moment it goes away, so the pill still has something to draw while it fades
    // out. A plain holder and not state: nothing here needs to recompose because of it.
    val last = remember { LastReadout() }
    if (readout != null) last.value = readout
    val shown = readout ?: last.value

    AnimatedVisibility(
        visible = readout != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        if (shown == null) return@AnimatedVisibility
        Row(
            modifier = Modifier
                .height(PillHeight)
                .background(VayouTheme.colors.videoPlate, VayouTheme.shapes.full)
                .padding(start = PillStart, end = PillEnd),
            horizontalArrangement = Arrangement.spacedBy(GlyphGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = shown.glyph,
                contentDescription = null,
                tint = VayouTheme.colors.onVideo,
                modifier = Modifier.size(GlyphSize),
            )
            // Fixed width and figures of one width, so the pill does not breathe as a number runs
            // through the tens.
            Box(modifier = Modifier.width(ValueWidth), contentAlignment = Alignment.Center) {
                Text(
                    text = shown.text,
                    // Only the figures are asked for: the rung already carries the size and the
                    // weight, and saying them again here was the one text size in the app that the
                    // scale did not have.
                    style = VayouTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = VayouTheme.colors.onVideo,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * What the picture is doing, said in the middle of it.
 *
 * No pill and no glyph: this stands where the transport stands, and while it is showing the
 * transport is not usable anyway -- a seek in progress, a zoom being pinched, a framing just
 * chosen. The eye is already on the picture, so the words go on the picture.
 */
@Composable
fun PlayerCentreReadout(text: String?, modifier: Modifier = Modifier) {
    val last = remember { LastText() }
    if (text != null) last.value = text

    AnimatedVisibility(visible = text != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text(
            text = text ?: last.value.orEmpty(),
            style = VayouTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = VayouTheme.colors.onVideo,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The level being set, or nothing.
 *
 * Only ever one: the two axes are the same finger going up or down, and it cannot be doing both.
 */
@Composable
fun playerReadout(axis: GestureAxis?, brightness: Float, volumePercent: Int): PlayerReadout? = when (axis) {
    GestureAxis.Brightness -> PlayerReadout(VayouIcons.Brightness, "${(brightness * Percent).toInt()}%")
    // Already a percentage when it arrives: past a hundred it is no longer a share of the range,
    // and working it out from the range here would read the amplified half as the whole.
    GestureAxis.Volume -> PlayerReadout(VayouIcons.VolumeUp, "$volumePercent%")
    GestureAxis.Seek, null -> null
}

/**
 * What to say in the middle, or nothing.
 *
 * Ordered by how recently the viewer asked for it: a framing they just chose outranks a run of
 * double taps, which outranks a drag along the film.
 */
@Composable
fun playerCentreReadout(
    axis: GestureAxis?,
    scale: VideoContentScale?,
    skipMs: Long,
    seekTargetMs: Long,
    /** Where a drag on the seek bar has reached, or null when no finger is on it. */
    scrubbingToMs: Long?,
): String? = when {
    scale != null -> stringResource(scale.label)
    skipMs != 0L -> stringResource(R.string.skip_readout, skipMs.milliseconds.inWholeSeconds)
    // Dragging the bar says where it has got to, the same way dragging across the picture does.
    // Both are the same question -- where will this land -- and the picture behind is a frame from
    // somewhere else until the finger lifts.
    scrubbingToMs != null -> formatTime(scrubbingToMs)
    axis == GestureAxis.Seek -> formatTime(seekTargetMs)
    else -> null
}

private class LastReadout {
    var value: PlayerReadout? = null
}

private class LastText {
    var value: String? = null
}

private const val Percent = 100

/** The app's 48dp floor: this is read at arm's length while a thumb is moving. */
private val PillHeight = 48.dp

/** Wide enough for "100%" and for a signed count of seconds, so neither changes the pill's width. */
private val ValueWidth = 64.dp

private val PillStart = 20.dp

private val PillEnd = 22.dp

private val GlyphGap = 8.dp

private val GlyphSize = 20.dp
