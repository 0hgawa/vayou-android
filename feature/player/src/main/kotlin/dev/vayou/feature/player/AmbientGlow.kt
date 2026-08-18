package dev.vayou.feature.player

import android.os.Build
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.Window
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.createBitmap
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.ui.graphics.dominantColour
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * The colour of the film, for the bars either side of it.
 *
 * A film almost never fills a phone, and what is left over is black on black -- the letterbox and
 * the backdrop behind it are the same nothing. Tinted with the picture's own colour, the bars stop
 * being a border and become part of the frame.
 *
 * One colour, found the way the music player finds the colour of a cover: the pixels are bucketed by
 * coarse hue and the heaviest bucket wins. A mean would be grey -- the warm and the cool cancel --
 * and a small grid stretched back over the screen bands, because between two cells of different
 * colour a linear ramp reads as a stripe.
 *
 * Read again every [SampleIntervalMs] while the film runs, not once and then never. Sampling only at
 * the start left the bars wearing the opening scene for two hours -- and, because a seek restarts the
 * renderer and so counts as a first frame, the colour moved when the bar was dragged and stood still
 * when the film played, which is the wrong way round. It is still nowhere near per-frame: a copy off
 * the GPU is a sync with it, and the way a browser follows a picture live is by drawing the video a
 * second time on the GPU, which on Android means giving up the SurfaceView and paying on every frame.
 *
 * Not composed at all when the setting is off, rather than composed and told to do nothing: the
 * listener, the loop and the copy all live in here, so leaving it out is the whole cost gone.
 *
 * The colour itself is animated rather than the opacity over it. A cut between two hues is a flash;
 * crossfading is what makes this read as light in a room instead of a background being swapped.
 */
@UnstableApi
@Composable
fun rememberAmbientGlow(player: Player, window: Window?): Color {
    // Black is off. Drawn over a black backdrop it is nothing, so there is no second value saying
    // whether to draw -- fading to black *is* fading out.
    var sampled by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(player, window) {
        // Until the first colour lands, ask again quickly. The film has not drawn anything yet when
        // this starts, so the first copy comes back empty -- and waiting out a whole interval for
        // that left the bars black for the opening seven seconds of every film.
        var hasColour = false
        while (true) {
            // A surface with nothing on it, a window on its way out, a scene that has faded to
            // black: all of them answer null, and the colour already up beats a jump to black.
            window?.videoSurface()?.sampleTint()?.let {
                sampled = it
                hasColour = true
            }
            delay(if (hasColour) SampleIntervalMs else FirstTryIntervalMs)
        }
    }

    // Black again the moment the film changes, so the last one's colour is not what the next is
    // first seen against.
    LaunchedEffect(player) {
        player.listen { events ->
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) sampled = Color.Black
        }
    }

    val glow by animateColorAsState(targetValue = sampled, animationSpec = tween(FadeMs), label = "ambient")
    return glow
}

/**
 * The view Media3 is drawing the film on, found rather than owned.
 *
 * The copy has to come from this and not from the window. A SurfaceView is composited as its own
 * layer, and where it sits the window's own buffer is a transparent hole -- copying the window
 * therefore succeeds and returns that hole, which is how this came back black while reporting
 * success. Media3's composable owns the view, so it is looked up instead of taken.
 */
private fun Window.videoSurface(): SurfaceView? {
    val decor = peekDecorView() as? ViewGroup ?: return null
    if (!decor.isAttachedToWindow) return null
    return decor.firstSurfaceView()
}

private fun ViewGroup.firstSurfaceView(): SurfaceView? {
    for (index in 0 until childCount) {
        when (val child = getChildAt(index)) {
            is SurfaceView -> return child
            is ViewGroup -> child.firstSurfaceView()?.let { return it }
        }
    }
    return null
}

/** The colour of the frame, from a copy small enough that finding it is free. */
private suspend fun SurfaceView.sampleTint(): Color? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    if (!holder.surface.isValid) return null

    val frame = createBitmap(SampleSize, SampleSize)
    val copied = suspendCancellableCoroutine { continuation ->
        try {
            PixelCopy.request(this, frame, { result -> continuation.resume(result) }, handler)
        } catch (_: IllegalArgumentException) {
            // The surface went away between the check above and the request.
            continuation.resume(PixelCopy.ERROR_SOURCE_INVALID)
        }
    }
    if (copied != PixelCopy.SUCCESS) {
        frame.recycle()
        return null
    }
    // Off the main thread: a thousand pixels is quick, but it is a loop and this runs the moment a
    // film settles, which is the moment the decoder is busiest.
    val colour = withContext(Dispatchers.Default) { frame.dominantColour()?.atLuminance(GlowLuminance) }
    frame.recycle()
    return colour
}

/** Small enough that the copy is cheap, large enough to find the hue that dominates. */
private const val SampleSize = 32

/**
 * The hue, at a brightness the eye actually reads.
 *
 * Not the HSL lightness the colour arrives with: that treats every hue alike and the eye does not.
 * At one lightness a yellow measures three times the luminance of a blue, so scene after scene the
 * warm ones came out glaring and the cool ones vanished. Scaled to a fixed luminance instead, every
 * film lands at the same brightness and only the hue changes.
 *
 * Scaled in linear light rather than on the encoded components. Luminance is a plain weighted sum of
 * the linear channels, so there the ratio is exact; applying it to the sRGB values instead and
 * calling it a 2.2 power overshot by forty per cent down here, where the encoding's straight
 * segment near black is most of the range.
 */
private fun Color.atLuminance(target: Float): Color {
    val current = luminance()
    if (current <= 0f) return this
    val scale = target / current
    return Color(
        red = linearToSrgb(srgbToLinear(red) * scale),
        green = linearToSrgb(srgbToLinear(green) * scale),
        blue = linearToSrgb(srgbToLinear(blue) * scale),
    )
}

private fun srgbToLinear(channel: Float): Float =
    if (channel <= SrgbKnee) channel / SrgbSlope else ((channel + SrgbOffset) / (1f + SrgbOffset)).pow(SrgbGamma)

private fun linearToSrgb(channel: Float): Float {
    val clamped = channel.coerceIn(0f, 1f)
    if (clamped <= SrgbKnee / SrgbSlope) return clamped * SrgbSlope
    return (1f + SrgbOffset) * clamped.pow(1f / SrgbGamma) - SrgbOffset
}

/**
 * How bright the glow is, and the only number to turn.
 *
 * Measured rather than guessed. The fixed-lightness version this replaced peaked at 0.023 on a warm
 * scene -- about a #2A2A2A grey -- while a cool one landed nearer 0.008, which is the unevenness the
 * luminance scaling exists to remove. This sits well under both, at roughly a #1A1A1A: a hue on the
 * bars rather than a light on them.
 */
private const val GlowLuminance = 0.010f

/** The sRGB transfer function, as the standard writes it: a straight segment near black and a
 *  2.4 power above it. */
private const val SrgbGamma = 2.4f

private const val SrgbKnee = 0.04045f

private const val SrgbSlope = 12.92f

private const val SrgbOffset = 0.055f

/**
 * How often the colour is read again.
 *
 * Three seconds: fast enough to follow the film rather than trail it. The risk at this rate is a
 * dialogue cut back and forth between two temperatures, which the bars would follow -- if that
 * shows, the answer is to move only when the colour has actually changed, not to slow this down.
 */
private const val SampleIntervalMs = 3_000L

/**
 * How soon to try again while there is still nothing to read.
 *
 * Only before the first colour: after that a black answer means the film itself has gone black, and
 * hammering the surface through a fade-out would be a copy off the GPU four times a second for a
 * scene that has no colour to give.
 */
private const val FirstTryIntervalMs = 250L

/** Long enough that a change of scene arrives as light moving rather than as a background swapped. */
private const val FadeMs = 1_500
