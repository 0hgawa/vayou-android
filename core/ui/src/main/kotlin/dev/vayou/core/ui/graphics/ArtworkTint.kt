package dev.vayou.core.ui.graphics

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The colour the now-playing screen dresses itself in, taken from the cover on it.
 *
 * Read from a 24-pixel copy rather than the full image: the answer is one colour, and decoding a
 * 3000px cover to average it would cost more than everything else the screen does. Coil caches the
 * request by size, so a track played twice never decodes twice, and the whole thing is skipped when
 * there is no artwork.
 *
 * The result is pushed towards a dark, half-saturated version of itself. A cover's own colour is
 * usually too bright or too vivid to sit behind text, and letting it through unchanged makes some
 * tracks unreadable and others black. The screen has to behave the same for every cover, so what
 * carries over is the hue, not the intensity.
 */
@Composable
fun rememberArtworkTint(model: Any?, fallback: Color): Color {
    val context = LocalContext.current
    var tint by remember(model) { mutableStateOf(fallback) }

    LaunchedEffect(model) {
        tint = model?.let { dominantColour(context, it) } ?: fallback
    }

    // Crossfaded, because a track change swaps the whole background: a hard cut between two hues
    // reads as a flash, and the cover itself is fading at the same time.
    val animated by animateColorAsState(
        targetValue = tint,
        animationSpec = tween(CrossfadeMs),
        label = "artworkTint",
    )
    return animated
}

private suspend fun dominantColour(context: Context, model: Any): Color? {
    val request = ImageRequest.Builder(context)
        .data(model)
        .size(SampleSize)
        // A hardware bitmap lives in graphics memory and cannot be read back pixel by pixel.
        .allowHardware(false)
        .build()
    val bitmap = (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap() ?: return null
    return withContext(Dispatchers.Default) { bitmap.dominantColour() }
}

/** Enough pixels to find the dominant hue, few enough that the scan is free. */
private const val SampleSize = 24

private const val CrossfadeMs = 500
