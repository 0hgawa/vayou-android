package dev.vayou.core.data.models

/**
 * Something played before, and how far into it the viewer got.
 *
 * Addressed rather than identified, because half of what a television plays has no identity to
 * give: a film on a share is known by where it lives and nothing else. Whoever draws it decides
 * what that address is worth showing.
 */
data class RecentPlayback(val uri: String, val positionMillis: Long, val durationMillis: Long) {
    /**
     * How much of it has been watched, or null where that cannot be said.
     *
     * Null and not zero: an entry recorded before lengths were kept has a position and no length,
     * and a bar drawn at zero would claim the film was never started when in fact it was.
     */
    val watched: Float? = (positionMillis.toFloat() / durationMillis)
        .takeIf { durationMillis > 0 && positionMillis > 0 }
        ?.coerceIn(0f, 1f)
}
