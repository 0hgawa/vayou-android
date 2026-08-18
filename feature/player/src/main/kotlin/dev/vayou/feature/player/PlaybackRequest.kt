package dev.vayou.feature.player

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle

/**
 * Everything an intent asked for, whether it came from the library or from another app.
 *
 * The extra names are MX Player's. They are not a standard anybody wrote down, but they are what
 * file managers, download apps and chat clients put in an intent when they hand a video to a
 * player, and the app has answered to them since before this rewrite. Refusing them now would be a
 * regression for people whose other apps already know how to talk to it.
 */
internal data class PlaybackRequest(
    val uri: String,
    val title: String?,
    /** Null to pick up where this file was last left; a number to start exactly there instead. */
    val startPositionMs: Long?,
    val subtitles: List<RequestedSubtitle>,
    /** The caller's own running order. Empty to use the folder the file sits in. */
    val queue: List<String>,
    /**
     * What to call each entry of [queue], by position.
     *
     * A file in the library can be looked up by its address; a channel cannot, because nothing in
     * this app has ever heard of it. Without these the bar over the second channel of an evening
     * would read as a URL.
     */
    val queueTitles: List<String>,
    /** The caller wants telling where playback got to when it ends. */
    val reportsResult: Boolean,
    /**
     * The caller knows this is a live channel, before anything has been loaded to prove it.
     *
     * Only a hint, and only for the first seconds: the player settles it for itself the moment a
     * timeline arrives. Without it the bar opens dressed for a film -- a clock, a seek bar, a queue,
     * a speed -- and rearranges itself in front of the viewer once the stream connects, which on a
     * channel is several seconds of the wrong screen followed by a jump.
     */
    val isLive: Boolean,
)

/** A subtitle the caller already has, named as it wants it listed. */
internal data class RequestedSubtitle(val uri: Uri, val name: String?, val isSelected: Boolean)

internal fun playbackRequestFrom(intent: Intent, resolveTitle: (Uri) -> String?): PlaybackRequest? {
    val uri = intent.getStringExtra(Extra.Uri) ?: intent.data?.toString() ?: return null
    val extras = intent.extras

    return PlaybackRequest(
        uri = uri,
        // Asked of whoever owns the file only when the caller did not say, since that is a query
        // against a content provider and the answer is usually already here.
        title = extras?.getString(Extra.Title) ?: intent.data?.let(resolveTitle),
        startPositionMs = extras?.takeIf { it.containsKey(Extra.Position) }?.getInt(Extra.Position)?.toLong(),
        subtitles = extras.requestedSubtitles(),
        queue = extras.uriArray(Extra.Queue).map(Uri::toString),
        queueTitles = extras?.getStringArray(Extra.QueueNames)?.toList().orEmpty(),
        reportsResult = extras?.containsKey(Extra.ReturnResult) == true,
        isLive = extras?.getBoolean(Extra.Live) == true,
    )
}

/**
 * What to hand back when playback ends, for a caller that asked to be told.
 *
 * Seconds are not offered and milliseconds are: the caller stores this to reopen the file later, and
 * rounding a resume point to the second is a second of the film watched twice.
 */
internal fun playbackResult(finished: Boolean, durationMs: Long, positionMs: Long): Intent =
    Intent(ResultAction).apply {
        if (finished) {
            putExtra(Extra.EndBy, EndByCompletion)
            return@apply
        }
        putExtra(Extra.EndBy, EndByUser)
        if (durationMs > 0L) putExtra(Extra.Duration, durationMs.toInt())
        if (positionMs >= 0L) putExtra(Extra.Position, positionMs.toInt())
    }

private fun Bundle?.requestedSubtitles(): List<RequestedSubtitle> {
    val uris = uriArray(Extra.Subtitles)
    if (uris.isEmpty()) return emptyList()

    val names = this?.getStringArray(Extra.SubtitleNames)
    val enabled = uriArray(Extra.SubtitlesEnabled).toSet()

    return uris.mapIndexed { index, uri ->
        RequestedSubtitle(
            uri = uri,
            name = names?.getOrNull(index),
            // A caller that names none leaves the first one on, which is what one subtitle means.
            isSelected = if (enabled.isEmpty()) index == 0 else uri in enabled,
        )
    }
}

@Suppress("DEPRECATION")
private fun Bundle?.uriArray(key: String): List<Uri> {
    this ?: return emptyList()
    val parcelables = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, Uri::class.java)
    } else {
        getParcelableArrayList<Uri>(key)
    }
    return parcelables.orEmpty()
}

private object Extra {
    /** The library's own, so it can pass a name it already knows without a provider query. */
    const val Uri = "uri"

    const val Title = "title"
    const val Position = "position"
    const val Duration = "duration"
    const val ReturnResult = "return_result"
    const val EndBy = "end_by"
    const val Subtitles = "subs"
    const val SubtitlesEnabled = "subs.enable"
    const val SubtitleNames = "subs.name"
    const val Live = "is_live"

    const val Queue = "video_list"

    /** Ours, not MX Player's -- it has no way to name a running order. Beside [Queue] as
     *  [SubtitleNames] is beside [Subtitles]. */
    const val QueueNames = "video_list.name"
}

private const val ResultAction = "com.mxtech.intent.result.VIEW"

private const val EndByUser = "user"

private const val EndByCompletion = "playback_completion"
