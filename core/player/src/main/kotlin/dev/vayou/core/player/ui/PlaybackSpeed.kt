package dev.vayou.core.player.ui

/**
 * `1×`, `1.5×`, `0.75×` — trailing zeroes dropped, because `1.50×` reads as more precision than a
 * speed has.
 *
 * Shared by both players: the number is what the button says on the phone, and a television that
 * wrote it differently would be a second answer to the same question.
 */
fun Float.asSpeedLabel(): String {
    val whole = toInt()
    return if (this == whole.toFloat()) "$whole×" else "%.2f".format(this).trimEnd('0').trimEnd('.', ',') + "×"
}
