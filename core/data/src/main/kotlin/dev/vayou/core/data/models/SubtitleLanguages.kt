package dev.vayou.core.data.models

/**
 * What language to ask OpenSubtitles for.
 *
 * Not [TranslationLanguages]: those are what a cue can be turned into and are ISO 639-1 two-letter
 * codes, while this endpoint speaks 639-2 three-letter ones and distinguishes European from
 * Brazilian Portuguese -- which for subtitles is the difference between readable and irritating.
 *
 * The blank id means every language, and leads because a viewer who has not said otherwise wants
 * whatever exists for the film in front of them.
 */
data class SubtitleLanguage(val id: String, val label: String)

val SubtitleLanguages: List<SubtitleLanguage> = listOf(
    SubtitleLanguage("", "Todos"),
    SubtitleLanguage("pob", "Portugues (BR)"),
    SubtitleLanguage("por", "Portugues"),
    SubtitleLanguage("eng", "English"),
    SubtitleLanguage("spa", "Espanol"),
    SubtitleLanguage("fre", "Francais"),
    SubtitleLanguage("ger", "Deutsch"),
    SubtitleLanguage("ita", "Italiano"),
    SubtitleLanguage("jpn", "Nihongo"),
    SubtitleLanguage("kor", "Hangugeo"),
    SubtitleLanguage("rus", "Russkiy"),
    SubtitleLanguage("chi", "Zhongwen"),
)
