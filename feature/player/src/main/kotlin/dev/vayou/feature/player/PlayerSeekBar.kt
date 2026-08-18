package dev.vayou.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.components.VayouSeekBar
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The film player's seek bar: [VayouSeekBar] in the colours a frame of video needs.
 *
 * Fixed white and the fixed accent rather than palette roles, for the reason the controls above it
 * give: this lies on a picture whose brightness nobody chose, so it carries its own contrast.
 */
@Composable
fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    /** A finger is on the bar. The player seeks to keyframes for as long as it is. */
    onScrub: (Boolean) -> Unit = {},
    /** Where a repeat begins and ends, drawn on the bar so the stretch can be seen and not guessed. */
    repeatFromMs: Long? = null,
    repeatToMs: Long? = null,
) {
    VayouSeekBar(
        fraction = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
        onSeekTo = { if (durationMs > 0) onSeek((it * durationMs).toLong()) },
        trackColor = VayouTheme.colors.onVideoMuted,
        activeColor = VayouTheme.colors.accentFixed,
        thumbColor = VayouTheme.colors.onVideo,
        modifier = modifier,
        onScrub = onScrub,
        marks = listOfNotNull(repeatFromMs, repeatToMs)
            .takeIf { durationMs > 0 }
            .orEmpty()
            .map { it.toFloat() / durationMs },
        markColor = VayouTheme.colors.accentFixed,
        height = BarLine,
    )
}

/**
 * What the block occupied before it became one component, so nothing around it moves.
 *
 * 48 and not 56: the old layout was 4dp of track inside 22dp of padding either side. The repeat
 * marks are 12dp, but they exist only once a stretch has been marked out, so they never set the
 * height in the ordinary case -- reading them as if they always did is what pushed the clock above
 * the bar up by eight.
 */
private val BarLine = 48.dp
