package dev.vayou.core.model

/** What two quick taps on the picture do. */
enum class DoubleTapGesture {
    /** Back on the left half, forward on the right. */
    SEEK,

    /** Play or pause, wherever it lands. */
    PLAY_PAUSE,

    /** Seek at the edges, play or pause in the middle third. */
    BOTH,

    /** Nothing, for a viewer whose taps keep being counted in pairs. */
    NONE,
}
