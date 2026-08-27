package dev.vayou.tv.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.media.Lyrics
import dev.vayou.tv.R

/**
 * The words of what is playing, beside the sleeve.
 *
 * Nothing here takes the focus, and that is the design rather than an omission. A remote has one
 * cursor, and a panel of forty lines that could each be landed on would swallow it: every press of
 * down would walk a verse instead of reaching the transport. The words follow the track by
 * themselves, which is the whole of what a listener wants from them on a television -- read, not
 * operated. The way out is the same left press that leaves the queue.
 *
 * A sheet whose lines are timed is followed line by line; one that is not is left still. Guessing
 * where an untimed sheet has got to would be a scroll the listener cannot trust, and a page that
 * moves for no reason is worse than a page that waits.
 */
@Composable
internal fun TvLyricsPanel(state: LyricsState, positionMs: Long, modifier: Modifier = Modifier) {
    if (state !is LyricsState.Found) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Nothing at all while disc is still answering: a track with no words and a track not
            // yet read look the same for a moment, and saying the wrong one of the two is worse
            // than saying nothing for that moment.
            if (state is LyricsState.None) {
                Text(
                    text = stringResource(R.string.no_lyrics),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    val lyrics: Lyrics = state.lyrics
    val listState = rememberLazyListState()

    // The last line that has started, which is the one being sung.
    val current = if (!lyrics.isTimed) -1 else lyrics.lines.indexOfLast { (it.atMs ?: 0) <= positionMs }

    // Held a third of the way down rather than at the top: a viewer reads ahead of the singer, and
    // a line pinned to the ceiling leaves nowhere to read to.
    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem(index = current, scrollOffset = -ReadAhead)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = ReadingInset),
        verticalArrangement = Arrangement.spacedBy(LineGap),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isNow = lyrics.isTimed && index == current
            Text(
                text = line.text,
                // The line being sung is the one the eye should find without hunting for it, so it
                // is larger as well as brighter: across a room, brightness alone is a weak signal.
                style = if (isNow) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (isNow || !lyrics.isTimed) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Room above the first line and below the last, so neither sits against the edge of the panel. */
private val ReadingInset = 24.dp

private val LineGap = 12.dp

/** How far down the panel the sung line is held, in pixels, so there is a page left to read. */
private const val ReadAhead = 220
