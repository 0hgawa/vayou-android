package dev.vayou.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Fixed on purpose: these lie on film, and film has no theme. */
private val OnVideo = Color(0xFFFFFFFF)

/** The unplayed half of a bar, and anything else that is present but not the subject. */
private val OnVideoMuted = Color(0x4DFFFFFF)

private val VideoContainer = Color(0x1AFFFFFF)

/**
 * One value where there were three -- the lock at 50%, the readout pill at 60%, the duration badge
 * at 60%. They are the same object seen in three places, so they read as one.
 */
private val VideoPlate = Color(0x99000000)

private val VideoScrim = Color(0x59000000)

private val VideoBackdrop = Color(0xFF000000)

/**
 * Every colour the app draws with, by the job the colour does.
 *
 * A data class, for two reasons that both pay: [withAccentFrom] and the pure-black palette become
 * a `copy` of the few fields that differ instead of thirty restated by hand, and the structural
 * equality lets Compose skip a subtree when a palette is rebuilt to the same values.
 */
@Immutable
data class VayouColors(
    /**
     * A tonal role, not the brand colour: dark in a light theme and light in a dark one, so that
     * [onAccent] flips with it and anything drawn on the accent reads at either end.
     *
     * The brand amber is a *light* colour -- luminance 0.535, which carries black at 9.70:1 and
     * white at 1.79:1. Used as the accent in a light theme it forced dark content, while Material's
     * own dynamic palette does the opposite there, and the two disagreed about what `onAccent`
     * means. Anything drawn on the accent came out one colour with the app's palette and another
     * with the wallpaper's.
     *
     * The exact brand amber did not go anywhere: it is [accentFixed], which is what the mark, the
     * folder and the progress bar take.
     */
    val accent: Color,
    /**
     * The accent at a value that does not move with the theme.
     *
     * Some things here are not surfaces reacting to the light around them. A folder is an object
     * with a colour of its own. The watched-progress bar lies on a frame of video, whose brightness
     * has nothing to do with which theme the app is in. Both keep one colour on white and on black.
     */
    val accentFixed: Color,
    /** The folder's deeper second panel. A step in hue, not in lightness, so a tint cannot reach it. */
    val folderTabColor: Color,

    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val scrim: Color,
    val onDisabled: Color,
    /**
     * What is legible on a frame of film, which is a different question from what is legible on a
     * surface of the app's own.
     *
     * These do not move with the theme, and that is the point: how bright a film is has nothing to
     * do with whether the reader chose dark mode. Every control over the picture was deriving this
     * for itself -- eighteen literal whites across seven files -- which is a design decision repeated
     * rather than named.
     */
    val onVideo: Color = OnVideo,
    val onVideoMuted: Color = OnVideoMuted,
    /** Behind a control on the picture, so a white glyph survives a white scene. */
    val videoContainer: Color = VideoContainer,
    /** A small plate on the picture -- the lock, the readout pill, the duration badge. */
    val videoPlate: Color = VideoPlate,
    /** Over the whole picture while the controls are up. */
    val videoScrim: Color = VideoScrim,
    /** Behind the picture, where the picture's shape does not fill the screen. */
    val videoBackdrop: Color = VideoBackdrop,
)

/**
 * These surfaces, [source]'s accent.
 *
 * The roles that carry the accent travel together: a slider filled from one palette with a thumb
 * from another is not a slider anyone drew.
 */
fun VayouColors.withAccentFrom(source: VayouColors) = copy(
    accent = source.accent,
    accentFixed = source.accentFixed,
    folderTabColor = source.folderTabColor,
    onAccent = source.onAccent,
    accentContainer = source.accentContainer,
    onAccentContainer = source.onAccentContainer,
)

/**
 * The brand amber, and the folder's deeper panel. Constants rather than the same hex repeated in
 * each palette, so "the same colour in every theme" is the code and not a comment two files apart.
 *
 * The dynamic palette says the same thing in Material's own words: `primaryFixedDim` is the role
 * defined to hold one value across the light and the dark scheme.
 */
private val AccentFixed = Color(0xFFFFB300)

/** The same amber two steps down Material's ramp, 600 to 800. */
private val FolderTab = Color(0xFFFF8F00)

val VayouLightColors = VayouColors(
    // The brand amber itself, the same value the dark theme takes.
    //
    // It could not be this while the accent also coloured text: #FFB300 on white is 1.79:1, so the
    // role had to be dragged down the ramp until it was legible, and an amber that dark is a brown.
    // Nothing draws text in it now -- headings, selected rows and breadcrumbs all say what they are
    // by weight or by a mark -- so the role is only ever a filled shape with dark content on it,
    // which is 9.70:1 and is what the folder has always been.
    accent = AccentFixed,
    accentFixed = AccentFixed,
    folderTabColor = FolderTab,
    // Dark on the amber, as in the dark theme: the accent is a light colour at both ends now, so
    // what sits on it is the same at both ends too.
    onAccent = Color(0xFF1A1A1A),
    accentContainer = Color(0xFFFFF3D6),
    onAccentContainer = Color(0xFF5C4200),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF49454E),
    // Each step sits a shade further from the surface it is drawn on, which on white means darker.
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    surfaceDim = Color(0xFFD9DADF),
    surfaceBright = Color(0xFFFFFFFF),
    outline = Color(0xFF72777F),
    outlineVariant = Color(0xFFC2C7CF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    scrim = Color(0xFF000000),
    onDisabled = Color(0xFF72777F),
)

val VayouDarkColors = VayouColors(
    // The brand amber itself here: on a near-black surface it reads at 10.76:1, and being a light
    // colour it carries dark content at 9.70:1. The light theme is the one that needs the step.
    accent = AccentFixed,
    accentFixed = AccentFixed,
    folderTabColor = FolderTab,
    onAccent = Color(0xFF1A1A1A),
    accentContainer = Color(0xFF4A3A0F),
    onAccentContainer = Color(0xFFFFE0A3),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF0E0E0E),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFCAC4C0),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    surfaceDim = Color(0xFF101316),
    surfaceBright = Color(0xFF36393E),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF484848),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    scrim = Color(0xFF000000),
    onDisabled = Color(0xFF8E8E8E),
)

/** The dark theme taken to true black, for OLED. Everything the depth ramp does not touch is the
 *  dark theme's own. */
val VayouPureBlackColors = VayouDarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF2A2A2A),
)

val LocalVayouColors = staticCompositionLocalOf { VayouDarkColors }
