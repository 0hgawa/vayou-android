package dev.vayou.feature.music

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.vayou.core.media.Song
import dev.vayou.core.player.queueKeys
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouPlayingIndicator
import dev.vayou.core.ui.designsystem.components.VayouSegmentedListItem
import dev.vayou.core.ui.designsystem.components.VayouSheet
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouToggleIconButton
import dev.vayou.core.ui.designsystem.components.draggedLift
import dev.vayou.core.ui.theme.VayouTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * What is queued, as a sheet.
 *
 * Deliberately self-contained -- it listens to the player itself rather than being fed state -- so
 * the mini bar can raise it over the library and the full player can raise it over itself, each
 * picking up the colours of whatever hosts it.
 *
 * The covers are looked up here rather than taken from the queue items. A queue item deliberately
 * carries no artwork -- MediaStore hands out an album-art address for every track that has an album
 * and it resolves to nothing for most of them -- so a row that trusted the item would be a blank
 * square, and only the track being played, whose cover the player has extracted, would show one.
 */
@Composable
fun QueueSheet(player: MediaController, onDismiss: () -> Unit) {
    var items by remember { mutableStateOf(player.queueItems()) }
    var currentIndex by remember { mutableIntStateOf(player.currentMediaItemIndex) }
    var isShuffled by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(active: Player, events: Player.Events) {
                items = active.queueItems()
                currentIndex = active.currentMediaItemIndex
                isShuffled = active.shuffleModeEnabled
                repeatMode = active.repeatMode
                isPlaying = active.isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val listState = rememberLazyListState()
    // Opening on track 40 of a long queue should not start at track 1.
    LaunchedEffect(Unit) { listState.scrollToItem(currentIndex.coerceAtLeast(0)) }

    val haptics = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        // The player owns the order, not this sheet: it moves the item and the listener above reads
        // the queue back, so what is on screen is what is queued rather than a copy hoping to agree.
        player.moveMediaItem(from.index, to.index)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    val keys = remember(items) { items.queueKeys() }

    val viewModel: MusicPlayerViewModel = hiltViewModel()
    var tracks by remember { mutableStateOf(emptyMap<String, Song>()) }
    LaunchedEffect(items) { tracks = viewModel.tracksFor(items.map { it.mediaId }) }

    val unknownArtist = stringResource(R.string.unknown_artist)

    VayouSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = VayouTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VayouSheetTitle(text = stringResource(R.string.queue), modifier = Modifier.weight(1f))
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

        // Capped rather than free to grow: a long queue would push the sheet to the top of the
        // screen, which hides the very thing it is a queue for. The same cap and the same spacing as
        // the video player's queue, which is the same sheet.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = VayouSheetDefaults.QueueMaxHeight),
            verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
        ) {
            itemsIndexed(items, key = { index, _ -> keys[index] }) { index, item ->
                ReorderableItem(state = reorderState, key = keys[index]) { isDragging ->
                    val song = tracks[item.mediaId]
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

                        title = song?.title?.takeIf { it.isNotBlank() }
                            ?: item.mediaMetadata.title?.toString().orEmpty(),
                        artist = song?.artist?.takeIf { it.isNotBlank() }
                            ?: item.mediaMetadata.artist?.toString()
                            ?: unknownArtist,
                        artworkUri = song?.artworkUri ?: item.mediaMetadata.artworkUri,
                        isCurrent = index == currentIndex,
                        isPlaying = isPlaying,
                        // Stays open on purpose: the marker moves to the tapped row, so picking
                        // through a queue is one gesture per track rather than reopening the sheet
                        // each time.
                        onClick = {
                            player.seekTo(index, 0L)
                            player.play()
                        },
                        dragHandle = {
                            Icon(
                                imageVector = VayouIcons.DragHandle,
                                contentDescription = stringResource(R.string.reorder),
                                // A step below the secondary role and a size below the icons that
                                // do something on a tap. This one only says the row can be moved,
                                // and on a list of forty it is drawn forty times: at full weight a
                                // column of grips reads as the loudest thing in the queue.
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
    title: String,
    artist: String,
    artworkUri: Uri?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    /**
     * The grip, on the trailing edge. Only it drags, so a tap anywhere else on the row still jumps
     * to the track -- which is what a queue is opened for.
     */
    dragHandle: @Composable () -> Unit,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        contentPadding = MediaListLayoutDefaults.SheetItemPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        onClick = onClick,
        trailingContent = dragHandle,
        leadingContent = {
            VayouArtwork(
                model = artworkUri,
                iconTint = VayouTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(MediaListLayoutDefaults.DenseLeadingSize),
                shape = VayouTheme.shapes.small,
            ) {
                // The playing track is marked on its cover rather than by recolouring the row, so
                // the queue keeps reading as a list of covers.
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VayouTheme.colors.scrim.copy(alpha = MarkerScrimAlpha)),
                        contentAlignment = Alignment.Center,
                    ) {
                        VayouPlayingIndicator(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(width = MarkerWidth, height = MarkerHeight),
                        )
                    }
                }
            }
        },
        content = {
            Text(
                text = title,
                style = VayouTheme.typography.bodyLarge,
                color = VayouTheme.colors.onSurface,
                // Weight rather than hue: this sheet opens over the library as well as over the
                // player, so it cannot count on a dark surface to make an amber line readable.
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = artist,
                // A step below the title, as the video rows' second line is. At the same size the
                // two read as one wrapped sentence and the eye has to pick out which half is the
                // name of the track.
                style = VayouTheme.typography.bodySmall,
                color = VayouTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

private fun Player.queueItems(): List<MediaItem> = List(mediaItemCount, ::getMediaItemAt)

private fun Int.nextRepeatMode(): Int = when (this) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

/** The same square a track row in the library uses, so the queue reads as that list reordered. */
private const val MarkerScrimAlpha = 0.55f

private val MarkerWidth = 18.dp

private val MarkerHeight = 16.dp
