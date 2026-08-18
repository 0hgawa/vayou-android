package dev.vayou.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.vayou.core.player.queueKeys
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.components.VayouBottomSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouMediaThumbnail
import dev.vayou.core.ui.designsystem.components.VayouSegmentedListItem
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouToggleIconButton
import dev.vayou.core.ui.designsystem.components.draggedLift
import dev.vayou.core.ui.theme.VayouTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * What is playing and what follows it.
 *
 * The video player's queue is its folder, in the order the library was showing -- which is what
 * makes "next" mean the row under the one you opened. This is where that becomes visible, and where
 * the order can be changed without leaving the player.
 *
 * The same sheet the music player has, down to the shuffle and repeat in its header and the grip on
 * each row: a queue is a queue, and a viewer who has learned one should not have to learn the other.
 */
@Composable
internal fun PlayerQueueSheet(player: Player, onDismiss: () -> Unit) {
    var items by remember { mutableStateOf(player.queue()) }
    var currentIndex by remember { mutableIntStateOf(player.currentMediaItemIndex) }
    var isShuffled by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(active: Player, events: Player.Events) {
                items = active.queue()
                currentIndex = active.currentMediaItemIndex
                isShuffled = active.shuffleModeEnabled
                repeatMode = active.repeatMode
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val keys = remember(items) { items.queueKeys() }
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        player.moveMediaItem(from.index, to.index)
    }

    // Opening on the fortieth film of a folder should not start at the first.
    LaunchedEffect(Unit) { listState.scrollToItem(currentIndex.coerceAtLeast(0)) }

    VayouBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = VayouTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VayouBottomSheetTitle(text = stringResource(R.string.queue), modifier = Modifier.weight(1f))
            VayouToggleIconButton(
                icon = VayouIcons.Shuffle,
                isOn = isShuffled,
                contentDescription = stringResource(R.string.shuffle),
                onClick = { player.shuffleModeEnabled = !isShuffled },
            )
            VayouToggleIconButton(
                icon = if (repeatMode == Player.REPEAT_MODE_ONE) VayouIcons.RepeatOne else VayouIcons.Repeat,
                isOn = repeatMode != Player.REPEAT_MODE_OFF,
                contentDescription = stringResource(R.string.repeat),
                onClick = { player.repeatMode = repeatMode.nextRepeatMode() },
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.heightIn(max = VayouSheetDefaults.QueueMaxHeight),
            verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
        ) {
            itemsIndexed(items, key = { index, _ -> keys[index] }) { index, item ->
                ReorderableItem(state = reorderState, key = keys[index]) { isDragging ->
                    QueueRow(
                        // Held anywhere on the row, not only on the grip. The grip is the visible
                        // way in; a hold is the one people try first, and in a queue nothing else
                        // is listening for it.
                        modifier = Modifier
                            .draggedLift(isDragging)
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                            ),
                        item = item,
                        isCurrent = index == currentIndex,
                        onPlay = {
                            player.seekTo(index, 0L)
                            player.play()
                        },
                        dragHandle = {
                            Icon(
                                imageVector = VayouIcons.DragHandle,
                                contentDescription = stringResource(R.string.reorder),
                                tint = VayouTheme.colors.outline,
                                modifier = Modifier
                                    .size(VayouTheme.iconSize.sm)
                                    .draggableHandle(
                                        onDragStarted = {
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.GestureThresholdActivate,
                                            )
                                        },
                                        onDragStopped = {
                                            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        },
                                    ),
                            )
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}

@Composable
private fun QueueRow(
    modifier: Modifier = Modifier,
    item: MediaItem,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    /**
     * The grip, on the trailing edge. Only it drags, so a tap anywhere else on the row still jumps
     * to the film -- which is what a queue is opened for.
     */
    dragHandle: @Composable () -> Unit,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        // The whole row lit, and nothing else. Bars dancing on the frame belong to the music sheet,
        // where the cover is a still image and something moving on it reads as sound; over a frame
        // of film they read as part of the film.
        selected = isCurrent,
        contentPadding = MediaListLayoutDefaults.SheetItemPadding,
        onClick = onPlay,
        trailingContent = dragHandle,
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                VayouMediaThumbnail(model = item.mediaId, modifier = Modifier.width(QueueThumbnailWidth))
                // Only on the one that is playing, and small. It is a marker, not a hint that
                // the tile is a film -- every row here is a film, so drawn on all of them it said
                // nothing and only crowded the frames. No plate under it either: on the row that
                // is playing the plate behind the whole line is already doing that work.
                if (isCurrent) {
                    // The same glyph twice: a dark copy a hair below, then the white one over it.
                    //
                    // A shadow rather than a plate behind it. White at any strength disappears on a
                    // bright frame -- this row's own thumbnail is a white kitchen -- and a disc
                    // under the mark was too heavy for something this small. An offset copy gives
                    // the edge it needs and stays a mark rather than becoming a badge.
                    Icon(
                        imageVector = VayouIcons.Play,
                        contentDescription = null,
                        tint = VayouTheme.colors.scrim.copy(alpha = PlayMarkShadowAlpha),
                        modifier = Modifier
                            .offset(y = PlayMarkShadow)
                            .size(PlayMarkSize),
                    )
                    Icon(
                        imageVector = VayouIcons.Play,
                        contentDescription = null,
                        tint = VayouTheme.colors.onVideo.copy(alpha = PlayMarkAlpha),
                        modifier = Modifier.size(PlayMarkSize),
                    )
                }
            }
        },
        content = {
            Text(
                text = item.mediaMetadata.title?.toString().orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

/** Every item the player is holding, in order. */
private fun Player.queue(): List<MediaItem> = List(mediaItemCount, ::getMediaItemAt)

private fun Int.nextRepeatMode(): Int = when (this) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

/** Narrower than the library's row: a sheet has less width to give, and this is a reminder of which
 *  film rather than a frame to study. */
private val QueueThumbnailWidth = 72.dp

private val PlayMarkSize = 16.dp

/** Quiet: the row it sits on is already lit, so this only has to confirm which frame. */
private const val PlayMarkAlpha = 0.9f

private val PlayMarkShadow = 1.dp

private const val PlayMarkShadowAlpha = 0.55f
