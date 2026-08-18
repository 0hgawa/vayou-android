package dev.vayou.tv

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The app's colours, in the set of components built for a remote control.
 *
 * Two themes at once, which looks like one too many and is not: the television's components come
 * from `tv-material3` and read their colours from its own [MaterialTheme], while everything shared
 * with the phone reads them from [VayouTheme]. Wrapping one in the other means the palette is still
 * declared in a single place and neither half is drawing colours the other has never heard of.
 *
 * Always dark. A television is watched in a dark room from three metres, and a light background is
 * a lamp pointed at the viewer.
 */
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    VayouTheme(darkTheme = true) {
        val colors = VayouTheme.colors
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = colors.videoBackdrop,
                surface = colors.videoBackdrop,
                // A step below what the phone uses for the same job. The phone's surfaces sit on
                // #0E0E0E; a television sits on true black, so every level of the ramp reads one
                // notch brighter here -- and a wall of cards at that level is a wall of grey
                // rectangles rather than pictures with somewhere to rest.
                surfaceVariant = colors.surfaceContainer,
                onSurfaceVariant = colors.onSurfaceVariant,
                // The mark's amber rather than the theme's accent: this one does not move with the
                // palette, and focus on a television has to be the same colour every time it lands.
                primary = colors.accentFixed,
                onPrimary = colors.onAccent,
                border = colors.accentFixed,
            ),
            content = content,
        )
    }
}
