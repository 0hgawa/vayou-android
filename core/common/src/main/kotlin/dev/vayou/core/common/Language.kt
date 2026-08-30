package dev.vayou.core.common

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * No language chosen, which is the set's own and where every reader starts.
 *
 * The app is offered in two, and most people who install it will already be reading the one their
 * television is in. The choice exists for the rest: a set sold in one country and lived in by
 * somebody from another.
 */
const val SystemLanguage = ""

/**
 * What the app is written in, each named in its own language.
 *
 * Not translated, and on purpose. Somebody looking at this list is looking at an app in a language
 * they would rather not read, and has to find theirs in it -- "Portuguese" is no help at all to a
 * reader who only knows the word Português.
 */
val AppLanguages: Map<String, String> = mapOf(
    // Alphabetical by the name as written, which is the order somebody scanning for their own
    // reads in. `id` and not `in`: the tag is the modern one, and Java maps it back to the old
    // code the resource folder carries.
    "id" to "Bahasa Indonesia",
    "de" to "Deutsch",
    "en" to "English",
    "es" to "Español",
    "fr" to "Français",
    "pt-BR" to "Português (Brasil)",
    "vi" to "Tiếng Việt",
    "tr" to "Türkçe",
    "ru" to "Русский",
    "ar" to "العربية",
    "hi" to "हिन्दी",
)

/**
 * The language the app is read in.
 *
 * Kept in a file of its own rather than with every other setting: a locale has to be known before
 * the first `Context` exists, and the store the rest live in answers on a coroutine. One value read
 * straight off the disk is something `attachBaseContext` can wait for; a stream is not.
 */
fun Context.appLanguage(): String = languageStore().getString(LanguageKey, SystemLanguage).orEmpty()

fun Context.setAppLanguage(tag: String) {
    languageStore().edit().putString(LanguageKey, tag).apply()
}

/**
 * This context, reading in the chosen language.
 *
 * [Locale.setDefault] as well as the configuration: the resources answer to the context, but a date
 * or a number formatted without one answers to the default, and the two disagreeing is how a screen
 * ends up half in each language.
 *
 * Nothing declares `android:localeConfig` alongside this, so the system offers no second place to
 * set the same thing. Two switches for one setting is how an app comes to disagree with itself.
 */
fun Context.inAppLanguage(): Context {
    val tag = appLanguage()
    if (tag == SystemLanguage) return this
    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(configuration)
}

private fun Context.languageStore() = getSharedPreferences(LanguageStore, Context.MODE_PRIVATE)

private const val LanguageStore = "language"

private const val LanguageKey = "tag"
