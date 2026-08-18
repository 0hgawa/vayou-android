package dev.vayou.core.player.ui

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale

/**
 * How a film that is not the screen's shape is fitted to it.
 *
 * The shape only, without a word for it: what each of these is called belongs to whichever shell is
 * showing the list, and the two of them keep their own strings.
 */
enum class VideoContentScale {
    /** The whole picture, with bars where the shapes disagree. */
    BestFit,

    /** The whole screen, at the cost of the picture's proportions. */
    Stretch,

    /** The whole screen, at the cost of the picture's edges. */
    Crop,

    /** One video pixel to one density-independent pixel, however small that leaves it. */
    HundredPercent,
    ;

    val next: VideoContentScale get() = entries[(ordinal + 1) % entries.size]

    fun toContentScale(): ContentScale = when (this) {
        BestFit -> ContentScale.Fit
        Stretch -> ContentScale.FillBounds
        Crop -> ContentScale.Crop
        HundredPercent -> FixedScale(1f)
    }
}
