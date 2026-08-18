package dev.vayou.core.smb

private val LogoAttribute = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)

private val GroupAttribute = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)

/**
 * The channels in an M3U playlist.
 *
 * Two lines make a channel: an `#EXTINF` carrying the name and the attributes, and the address
 * under it. Anything else -- comments, directives, blank lines -- is skipped rather than rejected,
 * because a playlist from the open internet has all three and refusing one costs the whole list.
 */
fun parseM3U(content: String): List<PlaylistChannel> {
    val channels = mutableListOf<PlaylistChannel>()
    val lines = content.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("#EXTINF")) {
            val url = lines.getOrNull(i + 1)?.trim()?.takeIf { it.isNotBlank() && !it.startsWith('#') }
            if (url != null) {
                val name = line.substringAfterLast(',').trim()
                channels += PlaylistChannel(
                    name = name.ifBlank { url },
                    url = url,
                    logo = LogoAttribute.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() },
                    group = GroupAttribute.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() },
                )
                i += 2
                continue
            }
        }
        i++
    }
    return channels
}
