package dev.vayou.core.ui.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Controls laid out along the film rather than along the reading.
 *
 * A timeline runs the same way in every language. The play glyph points one way, the bar fills one
 * way, and the button that goes forward belongs on the side the bar grows towards. In Arabic the
 * row mirrored and the bar did not -- the bar is a canvas painted by fraction of its own width,
 * which no layout direction reaches -- so "next" ended up over the part of the film already
 * watched, and one control was arguing with itself.
 *
 * Only the transport goes in here. Everything else on those screens is reading, and reading turns
 * over: the title, the sheets, the row of buttons that opens them.
 *
 * Free where it changes nothing: in a left-to-right language this hands down the value that was
 * already there.
 */
@Composable
fun AlongTheTimeline(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}
