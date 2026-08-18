package dev.vayou.core.ui.graphics

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

/**
 * The colour an image is mostly made of.
 *
 * Buckets the pixels by coarse RGB and returns the average of the heaviest bucket. A plain average
 * of every pixel is what turns most images to mud: opposite hues cancel and the answer is always a
 * grey-brown. Bucketing keeps the colour that occupies the most of the image, which is the one a
 * person would point at.
 *
 * Shared, because two screens want the same thing from two different pictures -- the cover behind
 * the music player, and the frame behind the film -- and the second one arrived at grey by
 * averaging before this was found.
 *
 * The result is pushed to a fixed [lightness] and a capped [maxSaturation]. An image's own colour is
 * usually too bright or too vivid to sit behind anything, and letting it through unchanged makes
 * some inputs unreadable and others black. What carries over is the hue, not the intensity.
 */
fun Bitmap.dominantColour(maxSaturation: Float = DefaultMaxSaturation, lightness: Float = DefaultLightness): Color? {
    // Read in one call: getPixel per coordinate crosses into native code every time.
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    val buckets = HashMap<Int, IntArray>()
    for (pixel in pixels) {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        // Near-black and near-white are an image's letterboxing and its paper, not its colour, and
        // counting them would win every bucket on half a library.
        if (maxOf(red, green, blue) < NearBlack || minOf(red, green, blue) > NearWhite) continue
        val key = (red shr 5 shl 10) or (green shr 5 shl 5) or (blue shr 5)
        val running = buckets.getOrPut(key) { IntArray(4) }
        running[0] += red
        running[1] += green
        running[2] += blue
        running[3]++
    }

    val heaviest = buckets.values.maxByOrNull { it[3] } ?: return null
    val count = heaviest[3]
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(heaviest[0] / count, heaviest[1] / count, heaviest[2] / count, hsl)
    hsl[1] = hsl[1].coerceAtMost(maxSaturation)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

private const val NearBlack = 24

private const val NearWhite = 232

const val DefaultMaxSaturation = 0.45f

/** Dark enough that text over it keeps its contrast whatever the picture is. */
const val DefaultLightness = 0.22f
