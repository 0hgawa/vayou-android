package dev.vayou.core.media

/**
 * How a music library is ordered.
 *
 * Every comparator reads low to high, so the arrow beside the axis always tells the truth: up is A
 * to Z, shortest first, oldest first. Down is the same comparator reversed, in one place.
 *
 * Carries no label: how an axis is named belongs to whoever draws it, in that reader's language,
 * and a string sitting here would be one set of words for the phone and the television both.
 */
enum class MusicSort(private val comparator: Comparator<Song>) {
    Title(compareBy(String.CASE_INSENSITIVE_ORDER, Song::title)),
    Artist(compareBy(String.CASE_INSENSITIVE_ORDER, Song::artist)),
    Album(compareBy(String.CASE_INSENSITIVE_ORDER, Song::album)),
    Duration(compareBy(Song::durationMs)),
    DateAdded(compareBy(Song::dateAddedSeconds)),
    ;

    fun ordering(isAscending: Boolean): Comparator<Song> = if (isAscending) comparator else comparator.reversed()
}
