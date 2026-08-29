package dev.vayou.core.smb

import java.util.Locale

/**
 * A country's channel list on iptv-org, or the whole index.
 *
 * Only the code is kept. The name of a country is something Android already knows in every language
 * it ships, so writing them down here would be writing down a translation table the platform hands
 * over for nothing -- and getting one of them wrong, or leaving it in one language, is exactly what
 * a written list does.
 */
data class IptvCountry(val code: String?) {
    val url: String
        get() = if (code.isNullOrBlank()) GlobalUrl else "$CountryPrefix${code.lowercase()}.m3u"

    /**
     * What to call this country to somebody reading in [locale].
     *
     * The locale is passed in rather than taken from the default: the language a viewer chose is
     * put on the screen's own resources, and the default is what the set is set to, which is not
     * the same thing.
     */
    fun nameIn(locale: Locale?): String? = code?.let {
        Locale.Builder().setRegion(it).build().getDisplayCountry(locale ?: Locale.getDefault())
    }

    companion object {
        const val CountryPrefix = "https://iptv-org.github.io/iptv/countries/"
        const val GlobalUrl = "https://iptv-org.github.io/iptv/index.m3u"
    }
}

/** The whole index first, then the countries this app offers, in the order they were chosen. */
val IptvCountries: List<IptvCountry> = listOf(
    IptvCountry(null),
    IptvCountry("br"),
    IptvCountry("pt"),
    IptvCountry("us"),
    IptvCountry("es"),
    IptvCountry("ar"),
    IptvCountry("mx"),
    IptvCountry("co"),
    IptvCountry("cl"),
    IptvCountry("uk"),
    IptvCountry("fr"),
    IptvCountry("de"),
    IptvCountry("it"),
    IptvCountry("nl"),
    IptvCountry("ca"),
    IptvCountry("jp"),
    IptvCountry("kr"),
    IptvCountry("cn"),
    IptvCountry("in"),
    IptvCountry("tr"),
    IptvCountry("ru"),
    IptvCountry("au"),
)
