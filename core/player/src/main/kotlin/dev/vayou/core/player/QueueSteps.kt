package dev.vayou.core.player

import androidx.media3.common.C
import androidx.media3.common.Player

/**
 * Stepping through a queue with its two ends joined.
 *
 * media3 stops at them unless the queue is set to repeat, which is the right answer for playback
 * left to run: a folder that finishes should finish. A press of the button is not that -- it is
 * somebody asking to move -- and one that answers nothing reads as broken. So the last item's next
 * is the first, and the first item's previous is the last.
 *
 * Only the buttons. What happens when a track runs out on its own is still the repeat setting's to
 * decide, and turning that into a loop is a different thing to ask for.
 *
 * Here rather than in either player, because the film, the song and the television all have the
 * same row of three buttons and there is one rule between them.
 */
fun Player.stepToNext() {
    when {
        hasNextMediaItem() -> seekToNext()
        // Guarded, or a single file would restart itself under a button that means "the next one".
        mediaItemCount > 1 -> seekTo(0, C.TIME_UNSET)
    }
}

/**
 * The item before this one, and the last again once there is none.
 *
 * Past the opening seconds it restarts what is playing instead, which is [Player.seekToPrevious]'s
 * own rule and the one every player follows. Only at the very start of the first item is there
 * nothing for it to do, and that is where the queue turns over.
 */
fun Player.stepToPrevious() {
    when {
        hasPreviousMediaItem() || currentPosition > maxSeekToPreviousPosition -> seekToPrevious()
        mediaItemCount > 1 -> seekTo(mediaItemCount - 1, C.TIME_UNSET)
    }
}
