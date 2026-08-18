package dev.vayou.core.model

/** Which way up the player asks the phone to be. */
enum class ScreenOrientation {
    /**
     * Turned by the sensor, ignoring the system's rotation lock.
     *
     * The lock exists so a page of text does not swing round while you read it in bed. A film is
     * the one thing you turn the phone *for*, which is why this and not "whatever the phone says"
     * is the default.
     */
    AUTOMATIC,

    /** Landscape, one way round only. */
    LANDSCAPE,

    /** Landscape, the other way round. */
    LANDSCAPE_REVERSE,

    /** Landscape, either way round. */
    LANDSCAPE_AUTO,

    /** Portrait, either way up. */
    PORTRAIT,

    /**
     * Landscape for a wide film and portrait for a tall one, taken from the picture itself.
     *
     * The one that needs nothing from the viewer: what shape a film is is a fact about the file,
     * and the phone can be turned to suit it without being asked.
     */
    VIDEO_ORIENTATION,
}
