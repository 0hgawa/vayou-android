package dev.vayou.tv

/**
 * A list the viewer built, with what it still points at rather than what it stores.
 *
 * An address outlives the file it names, so a count taken off the stored list reads high for ever;
 * these are resolved against the library once, where the query is, and the screens draw what came
 * back. Generic because the two libraries store the same thing about different media, and a second
 * copy of "id, name, and the items under it" would be a second place to keep them in step.
 *
 * On a television they are read-only. Making one, naming it and adding to it are a keyboard's work,
 * and that stays on the phone; what a remote is for is opening one and pressing play.
 */
data class TvMediaList<T>(val id: String, val name: String, val items: List<T>)
