package dev.vayou.core.smb

data class IptvCountry(val code: String?, val name: String) {
    fun urlOrNull(): String = if (code.isNullOrBlank()) GLOBAL_URL
                              else "$COUNTRY_PREFIX${code.lowercase()}.m3u"

    companion object {
        const val COUNTRY_PREFIX = "https://iptv-org.github.io/iptv/countries/"
        const val GLOBAL_URL = "https://iptv-org.github.io/iptv/index.m3u"
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
