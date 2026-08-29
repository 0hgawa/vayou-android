package dev.vayou.core.player.ui

import dev.vayou.core.model.PlayerPreferences

/**
 * Whole looks, picked in one tap.
 *
 * Copied from the old player, values and all. Six of them because subtitles fail in six different
 * ways: over a bright sky, over a dark room, on a phone at arm's length, on a television across
 * one. Reaching the same place through five separate switches is possible and nobody does it.
 *
 * In the order they are worth trying. The one the app ships with comes first, so a viewer who opens
 * this having changed nothing finds the tile that is already lit at the top left rather than hunting
 * for it. The four that leave the picture alone come before the two that lay a plate behind the
 * words -- a plate is the answer to a scene that defeats an outline, not the place to start.
 *
 * [Outlined] leads, where [Raised] used to. Raised is the two edge flags set at once, and media3
 * resolves that pair to an embossed letter rather than to an outline with a shadow behind it -- so
 * the tile at the top left was the one look here that shows no outline, on an app whose default is
 * named for having one.
 */
enum class SubtitlePreset(
    private val textColor: Int,
    private val background: Boolean,
    private val shadow: Boolean,
    private val outlineEnabled: Boolean,
    private val textBold: Boolean,
) {
    Outlined(
        textColor = White,
        background = false,
        shadow = false,
        outlineEnabled = true,
        textBold = true,
    ),

    Raised(
        textColor = White,
        background = false,
        shadow = true,
        outlineEnabled = true,
        textBold = true,
    ),

    DropShadow(
        textColor = White,
        background = false,
        shadow = true,
        outlineEnabled = false,
        textBold = true,
    ),

    Contrast(
        textColor = Yellow,
        background = false,
        shadow = false,
        outlineEnabled = true,
        textBold = true,
    ),

    Light(
        textColor = White,
        background = true,
        shadow = false,
        outlineEnabled = false,
        textBold = false,
    ),

    Box(
        textColor = White,
        background = true,
        shadow = false,
        outlineEnabled = false,
        textBold = true,
    ),
    ;

    fun applyTo(style: PlayerPreferences): PlayerPreferences = style.copy(
        subtitleTextColor = textColor,
        subtitleBackground = background,
        subtitleShadow = shadow,
        subtitleOutlineEnabled = outlineEnabled,
        subtitleTextBold = textBold,
    )

    fun matches(style: PlayerPreferences): Boolean = style.subtitleTextColor == textColor &&
        style.subtitleBackground == background &&
        style.subtitleShadow == shadow &&
        style.subtitleOutlineEnabled == outlineEnabled &&
        style.subtitleTextBold == textBold
}

/**
 * The style as it ships, put back.
 *
 * Only the subtitle fields: the reset in this sheet's corner undoes this sheet, not the equalizer
 * beside it or the gestures three sheets away.
 */
fun PlayerPreferences.withDefaultSubtitleStyle(): PlayerPreferences {
    val shipped = PlayerPreferences()
    return copy(
        subtitleTextSize = shipped.subtitleTextSize,
        subtitleTextBold = shipped.subtitleTextBold,
        subtitleTextColor = shipped.subtitleTextColor,
        subtitleBackground = shipped.subtitleBackground,
        subtitleOutlineEnabled = shipped.subtitleOutlineEnabled,
        subtitleOutlineColor = shipped.subtitleOutlineColor,
        subtitleShadow = shipped.subtitleShadow,
        subtitleVerticalPosition = shipped.subtitleVerticalPosition,
        useSystemCaptionStyle = shipped.useSystemCaptionStyle,
        applyEmbeddedStyles = shipped.applyEmbeddedStyles,
    )
}

/**
 * Asked of the reset rather than listed a second time: putting the defaults back and changing
 * nothing is what "already default" means, and a second list of ten fields is a list that drifts.
 */
val PlayerPreferences.isDefaultSubtitleStyle: Boolean
    get() = this == withDefaultSubtitleStyle()

/** The three sizes worth a tap. Anything between them is what the slider is for. */
enum class SubtitleSizePreset(val textSize: Int) {
    Small(16),
    Medium(20),
    Large(28),
}

/**
 * What a caption may be coloured, in the old player's order: the two that are read as absence of
 * colour first, then the ramp.
 *
 * Defined here and not beside the swatches because the presets pick from it too, and the two lists
 * had drifted -- the swatch yellow was `FFEB3B` and the preset's was `FFFF00`, so choosing "High
 * contrast" lit no swatch at all.
 */
val SubtitleColours = listOf(
    White,
    Black,
    0xFFFF1744.toInt(),
    0xFFFF6D00.toInt(),
    0xFFFFAB00.toInt(),
    Yellow,
    0xFF00E676.toInt(),
    0xFF00E5FF.toInt(),
    0xFF2979FF.toInt(),
    0xFFD500F9.toInt(),
    0xFFFF4081.toInt(),
)

const val White = 0xFFFFFFFF.toInt()

const val Black = 0xFF000000.toInt()

private const val Yellow = 0xFFFFFF00.toInt()
