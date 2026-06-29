package dev.vayou.core.smb

private val LogoRegex = Regex("""tvg-logo="([^"]*)"(?i)""")
private val GroupRegex = Regex("""group-title="([^"]*)"(?i)""")

fun parseM3U(content: String): List<PlaylistChannel> {
    val channels = mutableListOf<PlaylistChannel>()
    val lines = content.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("#EXTINF")) {
            val name = line.substringAfterLast(',').trim()
            val logo = LogoRegex.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            val group = GroupRegex.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            val url = lines.getOrNull(i + 1)?.trim()?.takeIf { it.isNotBlank() && !it.startsWith('#') }
            if (url != null) {
                channels.add(PlaylistChannel(name = name.ifBlank { url }, url = url, logo = logo, group = group))
                i += 2
                continue
            }
        }
        i++
    }
    return channels
}
