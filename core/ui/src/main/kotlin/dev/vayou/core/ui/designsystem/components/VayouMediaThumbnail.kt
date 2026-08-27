package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A frame of a video, with how long it runs and how much of it has been watched — the leading
 * visual of every video row, grid cell and queue entry.
 *
 * Takes a [model] the image loader can resolve and two already-formatted facts, never a `Video` or
 * a `MediaItem`: the same frame is drawn for a file in the library, an entry in the player's queue
 * and a row in the recents strip, and those are three different types describing one thing.
 *
 * The play symbol sits behind the frame rather than in an `else`: decoding a frame takes a moment
 * and can fail outright, and the tile has to read as a video either way.
 */
/**
 * Whether a thumbnail says how far into the video the viewer got.
 *
 * A setting, and one the reader turns off to stop the app announcing what they have watched, so it
 * has to reach every thumbnail on every screen -- the list, the grid, the strip along the top, the
 * search results. Handed down the composition rather than threaded through each of them: it is one
 * value, it changes about never, and passing it by hand meant a parameter on three components and
 * an argument at every place they are used, for a bar that is drawn in exactly one place.
 *
 * Shown unless something says otherwise, which is what a component drawn outside the app -- in a
 * preview, in a test -- should do.
 */
val LocalShowsPlayedProgress = staticCompositionLocalOf { true }

@Composable
fun VayouMediaThumbnail(
    model: Any?,
    modifier: Modifier = Modifier,
    /** Formatted by the caller — this component never sees a millisecond count. */
    duration: String? = null,
    /** 0f..1f. Zero hides the bar, which is what an unwatched video wants. */
    playedFraction: Float = 0f,
    aspectRatio: Float = DefaultAspectRatio,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(VayouTheme.shapes.small)
            .background(VayouTheme.colors.surfaceContainerHigh)
            .aspectRatio(aspectRatio),
    ) {
        Icon(
            imageVector = VayouIcons.Video,
            contentDescription = null,
            tint = VayouTheme.colors.surfaceContainerHighest,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(IconFraction),
        )

        if (model != null) {
            val request = remember(model) {
                ImageRequest.Builder(context)
                    .data(model)
                    // The frame arrives long after the row does; cutting to it is a flash.
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (duration != null) {
            VayouBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(BadgeInset),
                // Its own scrim, not a surface role: this sits on a frame we know nothing about,
                // and any palette colour would be legible over some videos and invisible over others.
                containerColor = VayouTheme.colors.videoPlate,
                contentColor = VayouTheme.colors.onVideo,
            ) {
                Text(text = duration)
            }
        }

        if (playedFraction > 0f && LocalShowsPlayedProgress.current) {
            Box(
                modifier = Modifier
                    .height(ProgressHeight)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                // Translucent white, exactly as the player draws the unplayed half of its seek bar,
                // and for the reason the badge above gives: this lies on a frame we know nothing
                // about. A surface role is a colour picked to sit next to other surfaces, and on a
                // photograph it reads as an opaque grey block -- with the palette dynamic, a tinted
                // one. White at half strength has no hue to clash with and darkens whatever it is on.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VayouTheme.colors.onVideo.copy(alpha = 0.5f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(playedFraction)
                        .fillMaxHeight()
                        // Rounded cap so the head of the bar reads as a position rather than as a
                        // block that happens to stop there.
                        .clip(CircleShape)
                        // The fixed accent, for the reason the track above gives: this bar lies on
                        // a frame of video, and how bright that frame is has nothing to do with
                        // which theme the app is in. The track was already fixed and the fill was
                        // not, so the two halves of one bar answered to different things.
                        .background(VayouTheme.colors.accentFixed),
                )
            }
        }
    }
}

/** Wider than 16:9 is tall: a library of frames reads as a column, not as a stack of screens. */
private const val DefaultAspectRatio = 16f / 10f

private const val IconFraction = 0.5f

private val BadgeInset = 4.dp

private val ProgressHeight = 4.dp
