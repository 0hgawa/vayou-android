package dev.vayou.feature.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vayou.core.media.Lyrics
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The words of what is playing, where the cover was.
 *
 * In the screen rather than in a sheet over it: the words are what the listener is looking at while
 * the track plays, and a panel that covers the transport turns reading them into a thing you leave
 * the player to do. Here the seek bar, the title and the buttons stay exactly where they were, and
 * only the square changes what it is showing.
 *
 * Timed words follow the playing: the line being sung is the bright one, the rest are dimmed, and
 * the list keeps it in view. Plain words are read at the reader's own pace, all in one weight and
 * nothing moving -- following a line the file never timed would be a guess, and a guess that
 * scrolls the page is worse than a page that stays put.
 */
@Composable
internal fun LyricsPanel(lyrics: Lyrics, positionMs: Long, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // The last line that has started, which is the one being sung. Walked rather than halved: two
    // lines can share an instant -- a chorus answering a verse -- and the last of them is the one
    // meant, which a binary search does not promise. A sheet is tens of lines long, and finding the
    // line that way costs less than the check that it was worth doing differently.
    val current = if (!lyrics.isTimed) -1 else lyrics.lines.indexOfLast { (it.atMs ?: 0) <= positionMs }

    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem(index = current)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = ReadingInset),
        verticalArrangement = Arrangement.spacedBy(VayouTheme.spacing.md),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isNow = lyrics.isTimed && index == current
            Text(
                text = line.text,
                style = VayouTheme.typography.titleMedium,
                color = if (isNow || !lyrics.isTimed) {
                    VayouTheme.colors.onSurface
                } else {
                    VayouTheme.colors.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Room above the first line and below the last, so neither sits against the edge of the panel. */
private val ReadingInset = 24.dp
