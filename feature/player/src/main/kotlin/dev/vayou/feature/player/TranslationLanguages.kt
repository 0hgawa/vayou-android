package dev.vayou.feature.player

import androidx.compose.runtime.Immutable

/**
 * What a subtitle can be turned into.
 *
 * A short list, not every language the endpoint knows. This is picked mid-film with a thumb, and a
 * hundred rows to scroll is a worse answer than the dozen anyone here is likely to want.
 */
@Immutable
data class TranslationLanguage(val code: String, val label: String)

val TranslationLanguages: List<TranslationLanguage> = listOf(
    TranslationLanguage("pt", "Portugues"),
    TranslationLanguage("en", "English"),
    TranslationLanguage("es", "Espanol"),
    TranslationLanguage("fr", "Francais"),
    TranslationLanguage("de", "Deutsch"),
    TranslationLanguage("it", "Italiano"),
    TranslationLanguage("ja", "Nihongo"),
    TranslationLanguage("ko", "Hangugeo"),
    TranslationLanguage("ru", "Russkiy"),
    TranslationLanguage("zh", "Zhongwen"),
)

/** Where the switch lands when it is first turned on. */
const val DefaultTranslationLanguage = "pt"
