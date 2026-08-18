package dev.vayou.feature.player

/**
 * `h:mm:ss` past the hour and `m:ss` below it, so a short clip is not padded out with a zero hour
 * that never changes.
 */
internal fun formatTime(millis: Long): String {
    val total = (millis.coerceAtLeast(0) + HalfSecond) / MillisPerSecond
    val seconds = total % SecondsPerMinute
    val minutes = (total / SecondsPerMinute) % MinutesPerHour
    val hours = total / SecondsPerHour
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Rounds to the nearest second, so a position at 0.9s does not read 0. */
private const val HalfSecond = 500L

private const val MillisPerSecond = 1000L

private const val SecondsPerMinute = 60L

private const val MinutesPerHour = 60L

private const val SecondsPerHour = 3600L
