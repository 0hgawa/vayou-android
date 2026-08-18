package dev.vayou.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.player.ui.SubtitlePreset
import dev.vayou.core.player.ui.SubtitleSample
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.tv.TvRowGap
import dev.vayou.tv.TvRowInset
import dev.vayou.tv.TvRowTick

/** What the panel is asking about. Some of these are questions, some are menus of questions. */
internal enum class TvSelector {
    Audio,
    Subtitle,
    SubtitleTracks,
    SubtitleDelay,
    Translation,
    SubtitleStyle,
    SubtitleSize,
    Speed,
    Playlist,
    More,
    Scale,
    Repeat,
    SleepTimer,
    VolumeBoost,
}

/**
 * One line of a list.
 *
 * [opens] rather than [onChoose] for a row that leads somewhere: a menu row does not answer
 * anything, and closing the panel on it would throw away the very step the viewer just took.
 */
internal data class TvSelectorOption(
    val label: String,
    val isSelected: Boolean = false,
    val subLabel: String? = null,
    val icon: ImageVector? = null,
    val opens: TvSelector? = null,
    /**
     * The caption style this row stands for, shown as three letters at the end of it.
     *
     * A name is a poor description of a look. The phone draws the same three letters on its preset
     * cards, and from a sofa the sample is the only part anybody reads.
     */
    val sample: SubtitlePreset? = null,
    val onChoose: () -> Unit = {},
)

/**
 * The list beside the film, in the space the film gives up for it.
 *
 * Not over the picture: opening this moves the film aside rather than covering it, which is what
 * the old player does and what a television wants. A sheet sliding up is a phone's answer, where a
 * thumb comes from below; here the hand holds a D-pad and the screen is the width of a wall, so the
 * two things sit side by side and both stay readable.
 *
 * Left or back closes it -- the key that would walk out of the column if it were a row.
 */
@Composable
internal fun TvPlayerSelector(
    title: String,
    options: List<TvSelectorOption>,
    onOpen: (TvSelector) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val selected = options.indexOfFirst { it.isSelected }.coerceAtLeast(0)

    // Opened on the answer that is already true, rather than at the top: the list of tracks in a
    // film runs past the foot of the screen, and the one in use is what a viewer is looking for.
    LaunchedEffect(title) {
        listState.scrollToItem(selected)
        runCatching { focus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key != Key.DirectionLeft && event.key != Key.Back) return@onPreviewKeyEvent false
                onDismiss()
                true
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(TitleGap),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(TvRowGap),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(options) { index, option ->
                    OptionRow(
                        option = option,
                        modifier = if (index == selected) Modifier.focusRequester(focus) else Modifier,
                        onClick = {
                            val opens = option.opens
                            if (opens != null) {
                                onOpen(opens)
                            } else {
                                option.onChoose()
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * One row, with room for a tick whether it carries one or not.
 *
 * The gutter is always there so the labels line up: a list where the chosen row starts further in
 * than the others reads as a mistake.
 */
@Composable
private fun OptionRow(option: TvSelectorOption, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TvRowInset, vertical = TvRowGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvRowInset),
        ) {
            Box(modifier = Modifier.size(TvRowTick), contentAlignment = Alignment.Center) {
                val mark = when {
                    option.isSelected -> VayouIcons.Check
                    else -> option.icon
                }
                if (mark != null) {
                    Icon(imageVector = mark, contentDescription = null, modifier = Modifier.size(TvRowTick))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // What the row currently says, under what the row is about: on a menu the answer is
                // the reason to walk into it, and reading it should not cost a press.
                option.subLabel?.let { under ->
                    Text(
                        text = under,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // On its own dark ground rather than the row's, which turns white under focus and would
            // swallow every style drawn in white letters.
            option.sample?.let { preset ->
                Box(
                    modifier = Modifier
                        .background(Color.Black, MaterialTheme.shapes.small)
                        .padding(horizontal = TvRowInset, vertical = SampleInset),
                ) {
                    SubtitleSample(preset)
                }
            }
        }
    }
}

private val TitleGap = 16.dp

private val SampleInset = 4.dp
