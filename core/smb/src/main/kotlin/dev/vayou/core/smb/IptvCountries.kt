package dev.vayou.core.smb

/**
 * A country's channel list on iptv-org, or the whole index.
 *
 * The names are not translated: a list of countries is read by looking for one's own, and "Brasil"
 * is what a reader in Brazil looks for whatever language the app is in.
 */
data class IptvCountry(val code: String?, val name: String) {
    val url: String
        get() = if (code.isNullOrBlank()) GlobalUrl else "$CountryPrefix${code.lowercase()}.m3u"

    companion object {
        const val CountryPrefix = "https://iptv-org.github.io/iptv/countries/"
        const val GlobalUrl = "https://iptv-org.github.io/iptv/index.m3u"
    }
}

val IptvCountries: List<IptvCountry> = listOf(
    IptvCountry(null, "Internacional"),
    IptvCountry("br", "Brasil"),
    IptvCountry("pt", "Portugal"),
    IptvCountry("us", "Estados Unidos"),
    IptvCountry("es", "Espanha"),
    IptvCountry("ar", "Argentina"),
    IptvCountry("mx", "México"),
    IptvCountry("co", "Colômbia"),
    IptvCountry("cl", "Chile"),
    IptvCountry("uk", "Reino Unido"),
    IptvCountry("fr", "França"),
    IptvCountry("de", "Alemanha"),
    IptvCountry("it", "Itália"),
    IptvCountry("nl", "Países Baixos"),
    IptvCountry("ca", "Canadá"),
    IptvCountry("jp", "Japão"),
    IptvCountry("kr", "Coreia do Sul"),
    IptvCountry("cn", "China"),
    IptvCountry("in", "Índia"),
    IptvCountry("tr", "Turquia"),
    IptvCountry("ru", "Rússia"),
    IptvCountry("au", "Austrália"),
)
