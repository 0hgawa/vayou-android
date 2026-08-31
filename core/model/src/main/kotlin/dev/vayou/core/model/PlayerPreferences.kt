package dev.vayou.core.model

import kotlinx.serialization.Serializable

/**
 * What the viewer has set for playback itself, as against for the library around it.
 *
 * Only what something reads. The old app's copy carries sixty-odd fields; each comes back here when
 * the control that sets it does, and not before — a preference nothing acts on is a value written
 * to disk for nobody.
 */
@Serializable
data class PlayerPreferences(
    // Playback itself.
    val resume: Resume = Resume.YES,
    val defaultPlaybackSpeed: Float = 1f,
    /** At the end of a film, carry on into the next one in the folder rather than stopping. */
    val autoplay: Boolean = true,
    /** True to shrink into the corner when the viewer leaves, rather than making them ask first. */
    val autoPip: Boolean = false,
    /** True to keep the sound going once the screen is gone, which is a lecture rather than a film. */
    val autoBackgroundPlay: Boolean = false,
    /** Levels the sound so quiet dialogue carries without the loud scenes waking the house. */
    val nightModeEnabled: Boolean = false,
    val playerScreenOrientation: ScreenOrientation = ScreenOrientation.AUTOMATIC,
    /** True to open the next film at [playerBrightness] rather than at whatever the phone is on. */
    val rememberPlayerBrightness: Boolean = false,
    val playerBrightness: Float = 0.5f,

    // The controls over the picture.
    /** Seconds of stillness before the controls go away. */
    val controllerAutoHideTimeout: Int = DefaultControllerTimeout,
    /**
     * True to sit every control on a translucent disc; false to draw the glyph alone on the film.
     *
     * The discs are what keep a white glyph legible over a bright scene without a scrim over the
     * whole picture. Off is the quieter look, for a viewer who would rather see the film.
     *
     * The film only. The music player's controls sit on a ground taken from the cover, and that
     * ground's lightness is imposed rather than read -- see `DefaultLightness`, which pins it dark
     * whatever the cover is. A white glyph there is legible by construction, so a disc under it
     * would be a shape that solves nothing.
     */
    val useMaterialYouControls: Boolean = true,
    /**
     * Whether the bars either side of the film take its colour.
     *
     * Off by default: it is the only thing on screen that is neither the film nor a control over
     * it, and a black surround is what a viewer expects of a player until they ask for something
     * else. It also costs a copy off the GPU every few seconds, which nothing else here does. The
     * switch is one tap away for whoever wants the frame tinted.
     */
    val useAmbientGlow: Boolean = false,

    // What a touch on the picture does.
    val useSeekControls: Boolean = true,
    val seekSensitivity: Float = DefaultGestureSensitivity,
    val enableBrightnessSwipeGesture: Boolean = true,
    val brightnessGestureSensitivity: Float = DefaultGestureSensitivity,
    val enableVolumeSwipeGesture: Boolean = true,
    /**
     * Whether the volume gesture may go past what the device gives, amplifying the rest.
     *
     * Off by default, as the old player had it. Amplification is not free: a mix that was already
     * loud enough comes out distorted, and that is a worse first impression than a film nobody
     * asked to be louder.
     */
    val enableVolumeBoost: Boolean = false,
    val volumeGestureSensitivity: Float = DefaultGestureSensitivity,
    val useZoomControls: Boolean = true,
    val doubleTapGesture: DoubleTapGesture = DoubleTapGesture.BOTH,
    /** Seconds a double tap or a skip button moves. */
    val seekIncrement: Int = DefaultSeekIncrement,

    // Sound, and who else on the phone gets a say in it.
    val decoderPriority: DecoderPriority = DecoderPriority.PREFER_DEVICE,
    /** False to talk over whatever else the phone is doing, rather than ducking and pausing for it. */
    val requireAudioFocus: Boolean = true,
    /** True to stop when the headphones come out, which is what stops a film playing to the room. */
    val pauseOnHeadsetDisconnect: Boolean = true,

    val equalizerEnabled: Boolean = false,
    val equalizerPreset: EqPreset = EqPreset.FLAT,
    /** Empty unless the curve was dragged by hand, in which case the preset is [EqPreset.CUSTOM]. */
    val equalizerBandGains: List<Int> = emptyList(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,

    // How subtitles look. The defaults are the old app's, so an upgrade changes nothing on screen.
    val subtitleTextSize: Int = DefaultSubtitleTextSize,
    val subtitleTextBold: Boolean = true,
    val subtitleFont: SubtitleFont = SubtitleFont.Default,
    val subtitleTextColor: Int = White,
    val subtitleBackground: Boolean = false,
    val subtitleOutlineEnabled: Boolean = true,
    val subtitleOutlineColor: Int = Black,
    // False, where the old app had it true, and this is the one default deliberately not inherited.
    // The two flags do not stack: outline and shadow together resolve to EDGE_TYPE_RAISED, which is
    // an embossed letter and not an outline at all, so the app shipped showing none of the black
    // outline it was configured for. Alone, the outline is the outline, which is what a caption
    // over film needs and what every tile in the sheet is drawn against.
    val subtitleShadow: Boolean = false,
    /** 0f at the foot of the picture, 1f at its top. */
    val subtitleVerticalPosition: Float = 0f,
    /** True to hand the whole question to Android's own captioning settings. */
    val useSystemCaptionStyle: Boolean = false,
    /** False to override the colours and positions an .ass file asks for. */
    val applyEmbeddedStyles: Boolean = true,
) {

    fun effectStrength(type: AudioEffectType): Int = when (type) {
        AudioEffectType.BASS_BOOST -> bassBoostStrength
        AudioEffectType.VIRTUALIZER -> virtualizerStrength
    }

    fun withEffectStrength(type: AudioEffectType, strength: Int): PlayerPreferences = when (type) {
        AudioEffectType.BASS_BOOST -> copy(bassBoostStrength = strength)
        AudioEffectType.VIRTUALIZER -> copy(virtualizerStrength = strength)
    }
}

/** Twenty, which is what a caption is on a phone held at arm's length. */
const val DefaultSubtitleTextSize = 20

/** Ten seconds, which is a line of dialogue missed. */
const val DefaultSeekIncrement = 10

/** Four seconds: long enough to read the row, short enough not to sit over the film. */
const val DefaultControllerTimeout = 4

/** The middle of the range every gesture is scaled by, so the default is "as it was". */
const val DefaultGestureSensitivity = 0.5f

private const val White = 0xFFFFFFFF.toInt()

private const val Black = 0xFF000000.toInt()
