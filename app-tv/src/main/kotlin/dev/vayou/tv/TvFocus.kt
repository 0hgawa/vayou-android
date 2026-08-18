package dev.vayou.tv

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

/**
 * Takes the focus, and keeps asking until [hasLanded] says it has.
 *
 * Asking once is not enough, and neither is asking until the call stops throwing. Two things go
 * wrong in the first few frames of any screen: the target does not exist yet, so the request is
 * dropped -- and once it does exist, Compose is still handing out the initial focus itself, which
 * goes to the first thing in reading order that will take it and overrides whatever was asked for a
 * moment earlier.
 *
 * What that costs is never nothing. On the music screen the first taker was the seek bar, so a
 * viewer pressing right to reach the next control scrubbed the track instead; in the options menu it
 * is nothing at all, and the remote is left with no way in.
 *
 * [hasLanded] is asked rather than assumed because a request that was accepted and then overridden
 * looks exactly like one that worked.
 */
suspend fun FocusRequester.claim(hasLanded: () -> Boolean) {
    repeat(Attempts) {
        withFrameNanos { }
        runCatching { requestFocus() }
        if (hasLanded()) return
    }
}

/**
 * Long enough to outlast a panel sliding in or a dialog being given a window, short enough that a
 * viewer never sees it. Frames rather than milliseconds because that is what the contest is: each
 * one is another chance for Compose to hand the focus somewhere else, and the last word has to be
 * ours.
 */
private const val Attempts = 30
