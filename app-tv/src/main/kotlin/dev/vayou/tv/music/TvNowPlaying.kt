package dev.vayou.tv.music

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.graphics.rememberArtworkTint
import dev.vayou.tv.Hairline
import dev.vayou.tv.R
import dev.vayou.tv.SplitMs
import dev.vayou.tv.TvCardGap
import dev.vayou.tv.TvCardMark
import dev.vayou.tv.TvCardTitleGap
import dev.vayou.tv.TvChoiceRow
import dev.vayou.tv.TvControlButton
import dev.vayou.tv.TvControlCapsule
import dev.vayou.tv.TvRowGap
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvSeekBar
import dev.vayou.tv.TvTickMs
import dev.vayou.tv.TvTitleInset
import dev.vayou.tv.WholeScreen
import dev.vayou.tv.claim
import dev.vayou.tv.tvClock
import kotlinx.coroutines.delay

/**
 * What is playing, as a screen of its own.
 *
 * Nothing here is borrowed from the video player, which is the whole point and the same point the
 * phone's now-playing screen makes: a film is watched and a track is listened to. There is no
 * picture to keep out of the way of, so the cover becomes the subject and the screen takes its
 * colour from it.
 *
 * Laid out across rather than down, unlike the phone. A television is wide and a sleeve is square:
 * stacked, the artwork would be a stamp with a wall of black either side of it.
 *
 * Everything shown is read off the player rather than passed in, so one screen serves a track from
 * this television's own library and a file on a share equally -- [known] is only what a caller
 * already has better than the tags, which is the library and nothing else.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvNowPlaying(controller: MediaController, known: TrackFacts? = null, sleeve: TvSleeveViewModel = hiltViewModel()) {
    var isPlaying by remember { mutableStateOf(controller.isPlaying) }
    var metadata by remember { mutableStateOf(controller.mediaMetadata) }
    var item by remember { mutableStateOf(controller.currentMediaItem) }
    var positionMs by remember { mutableLongStateOf(controller.currentPosition.coerceAtLeast(0)) }
    var durationMs by remember { mutableLongStateOf(controller.duration.coerceAtLeast(0)) }
    var repeatMode by remember { mutableIntStateOf(controller.repeatMode) }
    var isShuffling by remember { mutableStateOf(controller.shuffleModeEnabled) }
    var panel: TvSleevePanel? by remember { mutableStateOf(null) }

    // Held rather than read where it is needed. The count arrives after this screen does -- the
    // queue is handed over once the folder has answered -- and read straight off the controller it
    // was a plain number in the middle of a composition, so nothing recomposed when it changed. The
    // button that opens the list was drawn for a queue of one and stayed missing.
    var trackCount by remember { mutableIntStateOf(controller.mediaItemCount) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                metadata = mediaMetadata
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                item = mediaItem
                positionMs = controller.currentPosition.coerceAtLeast(0)
                durationMs = controller.duration.coerceAtLeast(0)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = controller.duration.coerceAtLeast(0)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                trackCount = controller.mediaItemCount
            }

            // Mirrored off the player rather than kept here, because this screen is not the only
            // thing that can change them: the phone and the notification are looking at the same
            // session, and a copy held on this side would be a second answer that goes stale.
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                isShuffling = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    // Ticked rather than listened for: a position has no event. Only while it is moving -- a paused
    // track is a wake-up twice a second for a number that is not changing.
    LaunchedEffect(controller, isPlaying) {
        while (isPlaying) {
            positionMs = controller.currentPosition.coerceAtLeast(0)
            durationMs = controller.duration.coerceAtLeast(0)
            delay(TvTickMs)
        }
    }

    // Whichever picture is there first is the one shown, and nothing replaces it while the track
    // lasts. Two arrive for one track -- the library has the album's straight away and the player
    // extracts the one inside the file a moment later -- and handing the second one over is what
    // makes the cover blink.
    val available = item?.mediaMetadata?.artworkData
        ?: item?.mediaMetadata?.artworkUri
        ?: known?.artwork
        ?: metadata.artworkData
        ?: metadata.artworkUri
    val settled = remember(item?.mediaId) { mutableStateOf<Any?>(null) }
    LaunchedEffect(item?.mediaId, available) { if (settled.value == null) settled.value = available }
    val cover = settled.value

    val title = known?.title?.takeIf { it.isNotBlank() }
        ?: metadata.title?.toString()?.takeIf { it.isNotBlank() }
        ?: item?.mediaId?.substringAfterLast('/').orEmpty()
    val artist = known?.artist?.takeIf { it.isNotBlank() }
        ?: metadata.artist?.toString()?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.unknown_artist)

    val surface = MaterialTheme.colorScheme.surface
    // A track with no cover still gets the wash, in the app's own grey rather than in the surface.
    // Falling back to the surface meant the gradient existed and did nothing -- black fading into
    // black -- so a file without artwork looked like a different screen from one with it.
    val tint = rememberArtworkTint(model = cover, fallback = MaterialTheme.colorScheme.surfaceVariant)

    // Asked for on every change of track, and only then: reading a tag is a file being opened,
    // and a sleeve that is up for the length of an album would otherwise ask again on every tick.
    val lyrics by sleeve.lyrics.collectAsStateWithLifecycle()
    LaunchedEffect(item?.mediaId) { sleeve.loadLyrics(item?.mediaId) }
    // A track without words takes its button away with it, and a panel whose way out has gone is a
    // panel the viewer is stuck in.
    LaunchedEffect(lyrics) {
        if (panel == TvSleevePanel.Lyrics && lyrics !is LyricsState.Found) panel = null
    }
    val preset by sleeve.preset.collectAsStateWithLifecycle()
    val isEqualizerOn by sleeve.isEqualizerOn.collectAsStateWithLifecycle()

    val play = remember { FocusRequester() }
    var isPlayFocused by remember { mutableStateOf(false) }

    // Whether the seek bar is allowed to take the focus yet, and the whole of how the focus lands
    // where it should.
    //
    // Asking for the play button was a race that kept being lost: Compose hands out the first focus
    // itself and gives it to the first thing in reading order that will take it, which on this
    // screen is the bar. Shut out for the first moment, there is nothing for that first assignment
    // to pick but a button, and the first button is play. It opens the instant the controls have
    // the focus, so a viewer can still walk down to it.
    //
    // What made the wrong answer worse than untidy: the bar acts on the arrow keys, and the key-up
    // left over from closing the queue reached it and committed a seek. The track rebuffered and the
    // button flicked from pause to play and back, which reads as the music stopping by itself.
    var isBarReachable by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        play.claim { isPlayFocused }
        isBarReachable = true
    }

    // Back to play when the queue closes, and not to the button it was opened from.
    //
    // Returning to the opener is the usual answer and it is the wrong one here: this screen has one
    // place the focus rests, and a viewer who has just closed a list is far likelier to want to stop
    // the music than to open the same list again. It is also the same place the focus starts, so
    // there is one rule to learn rather than two.
    var wasPanelOpen by remember { mutableStateOf(false) }
    LaunchedEffect(panel) {
        if (panel == null && wasPanelOpen) play.claim { isPlayFocused }
        wasPanelOpen = panel != null
    }

    // The queue arrives beside the sleeve rather than over it, the way the film player opens its
    // lists. A panel is where the focus can simply go -- something laid over the middle of the
    // screen has to take the focus away from what is underneath and give it back on the way out,
    // and every list that failed to do so left the remote stranded on the bar at the top.
    val isSplit = panel != null
    val sleeveWeight by animateFloatAsState(
        targetValue = if (isSplit) SleeveShare else WholeScreen,
        animationSpec = tween(SplitMs),
        label = "sleeve-weight",
    )
    val panelWeight by animateFloatAsState(
        targetValue = if (isSplit) PanelShare else Hairline,
        animationSpec = tween(SplitMs),
        label = "panel-weight",
    )
    val coverShare by animateFloatAsState(
        targetValue = if (isSplit) SplitCoverShare else CoverShare,
        animationSpec = tween(SplitMs),
        label = "cover-share",
    )

    // Everything but the cover, gathered so it can be put beside the square or under it without
    // being written twice.
    val words: @Composable (Modifier) -> Unit = { modifier ->
        Column(
            // A group, so the focus enters it once and walks within it, and so the queue panel
            // beside it is somewhere the focus goes rather than something it falls into.
            modifier = modifier
                .focusGroup()
                .focusProperties { enter = { play } },
            verticalArrangement = Arrangement.spacedBy(TvTitleInset),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvCardTitleGap),
            ) {
                Text(
                    text = tvClock(positionMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TvSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = controller::seekTo,
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties { canFocus = isBarReachable },
                )
                Text(
                    text = tvClock(durationMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(TvCardGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvControlButton(
                    icon = if (isPlaying) VayouIcons.PauseFilled else VayouIcons.Play,
                    label = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    onClick = { if (controller.isPlaying) controller.pause() else controller.play() },
                    modifier = Modifier
                        .focusRequester(play)
                        .onFocusChanged { isPlayFocused = it.isFocused },
                )
                TvControlCapsule {
                    TvControlButton(
                        icon = VayouIcons.SkipPreviousFilled,
                        label = stringResource(R.string.previous),
                        onClick = controller::seekToPreviousMediaItem,
                        isGrouped = true,
                    )
                    TvControlButton(
                        icon = VayouIcons.SkipNextFilled,
                        label = stringResource(R.string.next),
                        onClick = controller::seekToNextMediaItem,
                        isGrouped = true,
                    )
                }
                TvControlCapsule {
                    TvControlButton(
                        icon = VayouIcons.Shuffle,
                        label = stringResource(R.string.shuffle),
                        onClick = { controller.shuffleModeEnabled = !isShuffling },
                        isGrouped = true,
                    )
                    // One button and three states, as the phone has it: none, this one, all.
                    TvControlButton(
                        icon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                            VayouIcons.RepeatOne
                        } else {
                            VayouIcons.Repeat
                        },
                        label = stringResource(R.string.repeat_mode),
                        onClick = { controller.repeatMode = repeatMode.nextRepeatMode() },
                        isGrouped = true,
                    )
                }
                // Absent for a single track: a queue of one is a list with nothing to choose in.
                if (trackCount > 1) {
                    TvControlButton(
                        icon = VayouIcons.MusicPlaylist,
                        label = stringResource(R.string.queue),
                        onClick = { panel = panel.toggled(TvSleevePanel.Queue) },
                    )
                }
                // Only where there are words to read. A button that opens an empty panel is a
                // button that teaches the viewer to stop pressing it.
                // The button toggles rather than only opening, and for the words it is the only
                // way out: nothing in that panel takes the focus, so there is no left press for it
                // to consume the way the queue does. The other two toggle as well, because two
                // buttons that behave alike are one thing to learn.
                if (lyrics is LyricsState.Found) {
                    TvControlButton(
                        icon = VayouIcons.Subtitle,
                        label = stringResource(R.string.lyrics),
                        onClick = { panel = panel.toggled(TvSleevePanel.Lyrics) },
                    )
                }
                TvControlButton(
                    icon = VayouIcons.Equalizer,
                    label = stringResource(R.string.equalizer),
                    onClick = { panel = panel.toggled(TvSleevePanel.Equalizer) },
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            // Weighted to the left, where the cover is, so the colour sits behind the sleeve and the
            // surface returns under the words, where contrast has to be predictable. Across and not
            // down, because that is the way this screen is laid out.
            .background(
                Brush.horizontalGradient(
                    0f to tint,
                    GradientMidpoint to lerp(tint, surface, GradientMidBlend),
                    1f to surface,
                ),
            )
            .padding(TvScreenInset),
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().weight(sleeveWeight),
            horizontalArrangement = Arrangement.spacedBy(TvScreenInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // One arrangement, whether the queue is open or not. Every music player a television
            // has -- and every one this app is judged against -- puts the sleeve on the left and
            // what is playing on the right, and a screen that rearranged itself halfway through an
            // album would be two screens a viewer has to learn.
            //
            // Both halves are shares of whatever width is left rather than sizes, which is what
            // lets them give way together when the queue takes the right of the screen. Measured
            // off the width and not the height for the same reason: sized by height the square kept
            // every pixel it had and took the difference out of the column beside it, so the words
            // and the controls under them were the only things cut.
            Box(
                modifier = Modifier
                    .weight(coverShare)
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                TvCover(cover)
            }
            words(Modifier.weight(1f - coverShare))
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(panelWeight)
                .padding(start = if (isSplit) TvScreenInset else 0.dp),
        ) {
            when (panel) {
                TvSleevePanel.Queue -> Queue(controller, trackCount, onDismiss = { panel = null })
                TvSleevePanel.Lyrics -> TvLyricsPanel(state = lyrics, positionMs = positionMs)
                TvSleevePanel.Equalizer -> TvEqualizerPanel(
                    controller = controller,
                    current = preset,
                    isOn = isEqualizerOn,
                    onChosen = sleeve::rememberPreset,
                    onDismiss = { panel = null },
                )
                null -> Unit
            }
        }
    }
}

/**
 * What is coming, beside the sleeve, and a way straight to any of it.
 *
 * It stays open when a track is picked, unlike the film player's lists, and the difference is what
 * the two are. Those answer one question -- which subtitle, which speed -- and closing is the answer
 * being taken. A queue is a place: somebody who jumped to the fourth track is as likely to want the
 * ninth, and a panel that shut each time would make them reopen it and find their place again. The
 * sleeve behind it changes to the new track, so nothing is lost by leaving the list up.
 *
 * Left closes it, as well as back. The panel is to the right of everything, so walking off its left
 * edge is the way out, and a viewer holding left to get back to the controls should not have to find
 * the back key instead.
 */
@Composable
private fun Queue(controller: MediaController, trackCount: Int, onDismiss: () -> Unit) {
    val tracks = remember(trackCount) { List(trackCount) { index -> controller.getMediaItemAt(index) } }
    // Read as state rather than once, or the tick would move under a list that went on marking the
    // track the panel was opened on.
    var playing by remember { mutableIntStateOf(controller.currentMediaItemIndex) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                playing = controller.currentMediaItemIndex
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    // Where the focus is put on the way in, held so it does not move again: pinned to whatever is
    // playing, it would jump down the list under the viewer's hand each time they chose a track.
    val landing = remember { controller.currentMediaItemIndex.coerceAtLeast(0) }

    // Opened on what is playing rather than at the top, and scrolled to it first: a lazy list has
    // not composed the fortieth row, and a requester with nothing attached to it goes nowhere.
    LaunchedEffect(Unit) {
        listState.scrollToItem(landing)
        focus.claim { hasFocus }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Both halves of the press are taken, and acted on once. Consuming only the way down
            // left the way up to be delivered after the panel had gone -- to whatever had the focus
            // by then, which was the seek bar, which read it as a scrub and stuttered the track.
            .onPreviewKeyEvent { event ->
                if (event.key != Key.DirectionLeft && event.key != Key.Back) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) onDismiss()
                true
            },
        verticalArrangement = Arrangement.spacedBy(TvTitleInset),
    ) {
        Text(
            text = stringResource(R.string.queue),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(TvRowGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(tracks) { index, track ->
                TvChoiceRow(
                    label = track.mediaMetadata.title?.toString()
                        ?: Uri.decode(track.mediaId.substringAfterLast('/')),
                    isSelected = index == playing,
                    modifier = if (index == landing) {
                        Modifier
                            .focusRequester(focus)
                            .onFocusChanged { hasFocus = it.isFocused }
                    } else {
                        Modifier
                    },
                ) {
                    controller.seekTo(index, 0)
                    controller.play()
                }
            }
        }
    }
}

/**
 * The sleeve, or the app's own mark where there is none.
 *
 * Asked for rather than assumed: the library hands out an album-art address for every track that
 * belongs to an album and it resolves to nothing for most of them, so a square that trusted the
 * address to be a picture would be a grey rectangle with a name beside it.
 */
@Composable
fun TvCover(model: Any?) {
    val mark: @Composable () -> Unit = { TvCardMark(VayouIcons.AudioNotesFilled) }
    if (model == null) {
        mark()
        return
    }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        error = { mark() },
        modifier = Modifier.fillMaxSize(),
    )
}

/** What a caller already knows better than the file's own tags. The library, and nothing else. */
class TrackFacts(val title: String, val artist: String, val artwork: Any?)

private fun Int.nextRepeatMode(): Int = when (this) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

/** Where the cover's colour has given way, and how far it has given way by then. */
private const val GradientMidpoint = 0.45f

private const val GradientMidBlend = 0.65f

/** What the sleeve keeps of the width while the queue is beside it. */
private const val SleeveShare = 0.64f

private const val PanelShare = 0.36f

/**
 * How the sleeve is divided: the square, and everything else beside it.
 *
 * Two figures because the cover is what gives way when the queue opens. The column beside it cannot:
 * it carries six controls of the transport size with a gap between each, a shade under
 * 370dp, and that width is the same whatever else is on the screen.
 *
 * The arithmetic is worth writing down, because it is what these numbers are. A 1080p television
 * reports 960dp across at twice the density, less the screen's own inset, which leaves 864. With the
 * queue taking [PanelShare] the sleeve is 553 of that, and a cover on a fifth of it leaves the
 * column something over 430 -- enough, with room to spare. At the shares this screen started with,
 * the column got 307 and the last button was cut off by the panel.
 */
private const val CoverShare = 0.42f

private const val SplitCoverShare = 0.22f

/**
 * What the panel beside the sleeve is showing.
 *
 * One at a time, and one place for it: three panels that could each be open would be three widths
 * to animate between and a focus that has to be handed back from whichever of them had it.
 */
internal enum class TvSleevePanel {
    Queue,
    Lyrics,
    Equalizer,
}

/** Pressing the button that opened a panel closes it, which is the only way out of the words. */
private fun TvSleevePanel?.toggled(wanted: TvSleevePanel): TvSleevePanel? = if (this == wanted) null else wanted
