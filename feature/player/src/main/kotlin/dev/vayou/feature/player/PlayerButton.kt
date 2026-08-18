package dev.vayou.feature.player

import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.theme.VayouColors
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A control that sits on the picture.
 *
 * White and its own faint disc, not the palette: these lie on a frame whose colours nobody chose,
 * so each one carries the contrast it needs rather than borrowing it from a surface underneath.
 */
@Composable
fun PlayerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = PlayerButtonSize.Standard,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        // Dimmed rather than hidden. What is behind this is a frame of film, so the muted role from
        // the palette says nothing here -- the only thing that reads as "off" over an unknown
        // picture is less of the same white.
        LocalContentColor provides if (enabled) VayouTheme.colors.onVideo else VayouTheme.colors.onVideoMuted,
        LocalRippleConfiguration provides WhiteRipple,
    ) {
        VayouIconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            enabled = enabled,
            onLongClick = onLongClick,
            containerColor = if (LocalPlayerDiscs.current) VayouTheme.colors.videoContainer else Color.Transparent,
            content = content,
        )
    }
}

object PlayerButtonSize {
    /** Every control in the bars. Smaller than the 48dp floor because each one wears a disc. */
    val Standard = 40.dp

    /** Play alone, on either player. The one control a thumb goes to without looking. */
    val Primary = 76.dp

    /**
     * The two beside play. Its own token rather than [Standard], which is what the rows of bar
     * controls take: those are a row of equals, and these two answer to the disc between them.
     *
     * Both grew without their glyphs: what is drawn stays the size it reads well at, and the disc
     * around it is the part a thumb aims for.
     */
    val Secondary = 52.dp

    /**
     * The glyph inside [Primary], not the button around it: the target stays 64dp, so what a thumb
     * has to hit is unchanged and the ripple is still the same disc.
     *
     * At 48 it filled three quarters of its button, which over a picture read less as a control
     * than as a mark drawn on the video. Forty is five eighths.
     */
    val PrimaryGlyph = 40.dp

    /** For the bar controls, which are smaller than the disc they sit in. */
    val StandardGlyph = 22.dp
}

/**
 * Not a composable, so the colour cannot come from the theme here -- and it does not need to. A
 * ripple on film is white for the same reason [VayouColors.onVideo] is.
 */
private val WhiteRipple = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(0.5f, 0.5f, 0.5f, 0.5f),
)

/**
 * Whether the controls wear their discs.
 *
 * A composition local and not a parameter: it is one answer for every control on the screen, and
 * six call sites taking it are six chances for one of them to disagree.
 */
val LocalPlayerDiscs = staticCompositionLocalOf { true }
