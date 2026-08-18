package dev.vayou.tv

/**
 * A screen giving half of itself to a list beside it.
 *
 * Both players do this and they do it the same way: the film or the sleeve keeps a share of the
 * width, the panel takes the rest, and the two are animated so the list arrives rather than appears.
 * What differs between them is only how much the panel takes, which each one says for itself.
 */
const val WholeScreen = 1f

/** Not zero: a weightless box leaves the layout, and the panel would appear rather than arrive. */
const val Hairline = 0.0001f

const val SplitMs = 250
