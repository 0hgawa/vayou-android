package dev.vayou.core.model

/**
 * Which of the four faces every Android device carries a caption is set in.
 *
 * The platform's own and no more. A subtitle is read at a glance over a moving picture, and a font
 * shipped with the app would be one more thing to load before the first line appears -- for a
 * choice whose real answers are "the usual one", "one where every character is the same width" and
 * "one with serifs".
 */
enum class SubtitleFont {
    Default,
    SansSerif,
    Serif,
    Monospace,
}
