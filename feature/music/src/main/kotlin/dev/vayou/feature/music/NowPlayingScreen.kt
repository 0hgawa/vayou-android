package dev.vayou.feature.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.vayou.core.common.Utils
import dev.vayou.core.media.Lyrics
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.player.stepToNext
import dev.vayou.core.player.stepToPrevious
import dev.vayou.core.player.ui.isDescribed
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouArtworkRole
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouSeekBar
import dev.vayou.core.ui.graphics.rememberArtworkTint
import dev.vayou.core.ui.theme.VayouTheme
import dev.vayou.feature.player.EqualizerSheet
import dev.vayou.feature.player.PlayerButtonSize
import dev.vayou.feature.player.SleepTimerSheet
import dev.vayou.feature.player.rememberCastRouteName
import dev.vayou.feature.player.rememberEqualizerState
import dev.vayou.feature.player.rememberSleepTimerState
import kotlinx.coroutines.delay

/**
 * What is playing, as a screen of its own.
 *
 * Nothing here is borrowed from the video player, and that is the point: a film is watched and a
 * track is listened to. There is no picture to keep out of the way of, so the cover becomes the
 * subject, the screen takes its colour from that cover, and the controls sit on a surface rather
 * than floating over one.
 */
@Composable
fun NowPlayingScreen(
    player: MediaController?,
    preferences: PlayerPreferences,
    onSavePreferences: (PlayerPreferences.() -> PlayerPreferences) -> Unit,
    onBack: () -> Unit,
    /** Everything that can be done to the track playing, behind the key opposite the chevron. */
    menu: @Composable () -> Unit = {},
    /** The star, at the far end of the title's line -- the one action here that gets repeated. */
    favourite: @Composable () -> Unit = {},
    /** The words of what is playing, or null for a track that has none. */
    lyrics: Lyrics? = null,
) {
    // The screen is dressed by whatever is playing. It reads the cover the content decided to show
    // rather than resolving the artwork twice -- the metadata listeners live one level down.
    var artwork by remember { mutableStateOf<Any?>(null) }
    val surface = VayouTheme.colors.surface
    val tint = rememberArtworkTint(model = artwork, fallback = surface)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Top-weighted, so the colour sits behind the cover and the surface returns under
                // the controls, where contrast has to be predictable.
                Brush.verticalGradient(
                    0f to tint,
                    GradientMidpoint to lerp(tint, surface, GradientMidBlend),
                    1f to surface,
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            // The same inset the film player's bar takes below the status bar. Without it the keys
            // sat against the clock here and a finger's width lower there, and the two players read
            // as two apps at the moment you leave one for the other.
            modifier = Modifier.padding(horizontal = ButtonRowPadding, vertical = VayouTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A chevron down, not an arrow back: this screen came up from the bottom, and it goes
            // back down. The arrow is what leaves a place you navigated into.
            VayouIconButton(onClick = onBack) {
                Icon(
                    imageVector = VayouIcons.ChevronDown,
                    contentDescription = stringResource(R.string.back),
                    tint = VayouTheme.colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            menu()
        }

        if (player == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VayouCircularProgress()
            }
            return@Column
        }

        NowPlaying(
            player = player,
            preferences = preferences,
            onSavePreferences = onSavePreferences,
            onArtworkChange = { artwork = it },
            favourite = favourite,
            lyrics = lyrics,
        )
    }
}

@Composable
private fun NowPlaying(
    player: MediaController,
    preferences: PlayerPreferences,
    onSavePreferences: (PlayerPreferences.() -> PlayerPreferences) -> Unit,
    onArtworkChange: (Any?) -> Unit,
    favourite: @Composable () -> Unit,
    lyrics: Lyrics?,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var isShuffled by remember { mutableStateOf(player.shuffleModeEnabled) }
    var playerMetadata by remember { mutableStateOf(player.mediaMetadata) }
    var currentItem by remember { mutableStateOf(player.currentMediaItem) }
    var positionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0)) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var isDragging by remember { mutableStateOf(false) }
    var draggedMs by remember { mutableFloatStateOf(0f) }
    var isEqualizerOpen by remember { mutableStateOf(false) }
    var isSleepTimerOpen by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isLyricsOpen by remember { mutableStateOf(false) }
    // One value rather than a flag and a nullable read apart: the panel is open only while there
    // is something in it, so a track change to one without words puts the cover back instead of
    // leaving an empty page.
    val shownLyrics = lyrics?.takeIf { isLyricsOpen }

    val sleepTimer = rememberSleepTimerState(player)
    val equalizer = rememberEqualizerState(player, preferences, onSavePreferences)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                isShuffled = enabled
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                playerMetadata = mediaMetadata
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentItem = mediaItem
                playerMetadata = player.mediaMetadata
                positionMs = player.currentPosition.coerceAtLeast(0)
                durationMs = player.duration.coerceAtLeast(0)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                durationMs = player.duration.coerceAtLeast(0)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Ticked rather than listened for: a position has no event, and half a second is finer than the
    // eye reads a bar this wide.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isDragging) positionMs = player.currentPosition.coerceAtLeast(0)
            durationMs = player.duration.coerceAtLeast(0)
            delay(TickMs)
        }
    }

    // Title and artist come from the store, which stays right even when a file's own tags are
    // empty.
    val itemMetadata = currentItem?.mediaMetadata

    // Whichever picture is there first is the one shown, and nothing replaces it while the track
    // lasts.
    //
    // Two arrive for one track: the store has the album's the moment the screen is up, and the
    // player extracts the one inside the file about a third of a second later. Handing the second
    // one over is what made the cover blink -- the image clears while the new one is read and the
    // mark behind shows through, so it appeared, vanished and came back at the start of every
    // track. The file's picture is the truer one, being this track's rather than its album's, but
    // a third of a second of that is not worth the flicker.
    // The item's own picture, which it has carried since the queue was built. One source and no
    // clock: it cannot lag behind the track, so there is nothing to hold still and nothing to
    // settle. The player's extraction is only for what the item could not name -- a stream, whose
    // picture arrives with the first frames of audio.
    val cover = itemMetadata?.artworkUri
        ?: itemMetadata?.artworkData
        ?: playerMetadata.artworkData
        ?: playerMetadata.artworkUri
    LaunchedEffect(cover) { onArtworkChange(cover) }

    // The item's own words, for the same reason: they were put there from the library when the
    // queue was built, so they are the store's answer without the store being asked again.
    val title = itemMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
        ?: playerMetadata.title?.toString()?.takeIf { it.isNotBlank() }
        ?: currentItem?.localConfiguration?.uri?.lastPathSegment
        ?: ""
    // Blank rather than "unknown" until one of the two has described the track: said before either
    // has answered it is said wrongly, and the line corrects itself in front of the listener.
    val artist = itemMetadata?.artist?.toString()?.takeIf { it.isNotBlank() }
        ?: playerMetadata.artist?.toString()?.takeIf { it.isNotBlank() }
        ?: if (itemMetadata?.isDescribed == true || playerMetadata.isDescribed) {
            stringResource(R.string.unknown_artist)
        } else {
            ""
        }

    // Gathered into one value because it all changes together and has to *move* together: the
    // sliding panes read what they show from this, not from the screen around them, or the outgoing
    // one would be re-drawn with the incoming track's words halfway through leaving.
    val face = TrackFace(currentItem?.mediaId, cover, title, artist)

    val shownMs = if (isDragging) draggedMs else positionMs.toFloat()
    val onSeekFinished = {
        player.seekTo(draggedMs.toLong())
        positionMs = draggedMs.toLong()
        isDragging = false
    }

    // Split by the shape of the space, not by the orientation flag: a tall window on a foldable or
    // in split screen wants the stacked layout even when the device calls itself landscape.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ContentPadding)
                    .padding(bottom = VayouTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(WideGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (shownLyrics != null) {
                    LyricsPanel(
                        lyrics = shownLyrics,
                        positionMs = shownMs.toLong(),
                        modifier = Modifier.fillMaxHeight().weight(1f),
                    )
                } else {
                    // Bounded by the short side, not the long one: sized from the width, the square
                    // would be taller than the window and lose its bottom.
                    Cover(face.cover, Modifier.fillMaxHeight())
                }
                Column(modifier = Modifier.weight(1f)) {
                    TrackLine(face, favourite)
                    // Stripped to what a glance needs. The two times are dropped rather than
                    // squeezed -- a wide window is short, and the handle already says where the
                    // track is.
                    Progress(shownMs, durationMs, showTimes = false, onSeek = {
                        isDragging = true
                        draggedMs = it
                    }, onSeekFinished = onSeekFinished)
                    Transport(player, isPlaying, isShuffled, sleepTimer.isArmed) {
                        isSleepTimerOpen = true
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = ContentPadding),
                ) {
                    // Off the top of the screen, and by a share of what is spare rather than a
                    // fixed gap: pinned under the chevron the square sat against the bar on a tall
                    // phone and in the middle of a short one. A third above and two thirds below
                    // keeps it high enough to be the subject and low enough not to touch the bar.
                    // With the words open the name goes above them, where it names what is being
                    // read; with the cover it stays under the square, where it names what is being
                    // looked at. Either way the seek bar below does not move.
                    if (shownLyrics != null) {
                        TrackLine(face, favourite)
                        LyricsPanel(
                            lyrics = shownLyrics,
                            positionMs = shownMs.toLong(),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(CoverHeadroom))
                        // Just inside the full width, and centred on it. Edge to edge the square is
                        // the tallest thing on the screen by a distance, and a margin of its own
                        // parts it from the text that starts on the column's.
                        Cover(
                            artwork = face.cover,
                            modifier = Modifier
                                .fillMaxWidth(CoverWidthFraction)
                                .align(Alignment.CenterHorizontally),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TrackLine(face, favourite)
                    }
                    Progress(shownMs, durationMs, showTimes = true, onSeek = {
                        isDragging = true
                        draggedMs = it
                    }, onSeekFinished = onSeekFinished)
                }
                Transport(player, isPlaying, isShuffled, sleepTimer.isArmed) { isSleepTimerOpen = true }
                SecondaryActions(
                    onEqualizer = { isEqualizerOpen = true },
                    onQueue = { isQueueOpen = true },
                    onLyrics = lyrics?.let { { isLyricsOpen = !isLyricsOpen } },
                )
            }
        }
    }

    if (isEqualizerOpen) {
        equalizer?.let { EqualizerSheet(state = it, onDismiss = { isEqualizerOpen = false }) }
    }
    if (isSleepTimerOpen) {
        SleepTimerSheet(state = sleepTimer, onDismiss = { isSleepTimerOpen = false })
    }
    if (isQueueOpen) {
        QueueSheet(player = player, onDismiss = { isQueueOpen = false })
    }
}

/**
 * The cover, square, dissolving where it stands instead of travelling.
 *
 * Its placeholder is fainter and smaller than a row's: here the artwork is the subject of the
 * screen, so the fallback has to read as an absence rather than as the thing being shown.
 *
 * It used to slide with the words, and neither ending was right: clipped to its box, the half still
 * outside was cut by a straight edge and the square arrived with square corners; unclipped, the two
 * covers were drawn whole and read as one long strip sliding past. A picture has no direction to
 * move in anyway -- the words say which way the queue went. So the image loader crossfades this one
 * in over the last, which is what a cover does in every player that gets it right, and costs a
 * fade rather than two panes and a clip.
 */
@Composable
private fun Cover(artwork: Any?, modifier: Modifier = Modifier) {
    // The square says where the sound is when the sound is not here. It is the one part of this
    // screen that stops being true on a speaker in another room -- the words and the transport
    // still describe what is playing -- and it is the part the video player replaces for the same
    // reason: there is nothing to look at on this device.
    val room = rememberCastRouteName()
    if (room != null) {
        CastCover(room, modifier)
        return
    }
    VayouArtwork(
        model = artwork,
        iconTint = VayouTheme.colors.onSurfaceVariant,
        modifier = modifier.aspectRatio(1f),
        role = VayouArtworkRole.Ghost,
        shape = VayouTheme.shapes.largeIncreased,
    )
}

/** The cover's place while a television or a speaker has the track: what has it, and its name. */
@Composable
private fun CastCover(room: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(VayouTheme.shapes.largeIncreased)
            .background(VayouTheme.colors.surfaceContainer)
            .padding(ContentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = VayouIcons.CastConnected,
            contentDescription = null,
            tint = VayouTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(CastGlyphSize),
        )
        Spacer(modifier = Modifier.height(VayouTheme.spacing.md))
        Text(
            text = room,
            style = VayouTheme.typography.titleMedium,
            color = VayouTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Everything on the screen that belongs to the track rather than to the playing of it.
 *
 * One value rather than three arguments, because the panes that slide have to keep showing what
 * they were showing while they leave.
 */
/**
 * The star and the words, on one line.
 *
 * Written once and used by both layouts: the pane that slides is the same in each, and a second
 * copy of this arrangement is where the two of them would start to drift apart.
 */
@Composable
private fun TrackLine(face: TrackFace, favourite: @Composable () -> Unit) {
    // Bottom, so the star lands on the artist's line rather than between the two: the name is the
    // subject and the star is a remark about it, and level with the quieter line it stops competing
    // with the title for the eye.
    Row(verticalAlignment = Alignment.Bottom) {
        Box(modifier = Modifier.weight(1f)) {
            DissolvingTrack(face) { TrackInfo(it.title, it.artist) }
        }
        favourite()
    }
}

@Immutable
private data class TrackFace(val id: String?, val cover: Any?, val title: String, val artist: String)

/**
 * The words, dissolving where they stand.
 *
 * The same change the cover makes, because they are one thing changing: a track. They used to
 * travel sideways while the picture dissolved, and the two readings of a single event fought each
 * other -- the eye followed the words off the screen and found the cover already replaced. A
 * picture has no direction to move in, so the words gave up theirs.
 *
 * Short rather than medium: a dissolve says what it has to say in half the time a journey needs,
 * and at the medium length it reads as a hesitation.
 */
@Composable
private fun DissolvingTrack(face: TrackFace, modifier: Modifier = Modifier, content: @Composable (TrackFace) -> Unit) {
    val motion = VayouTheme.motion
    AnimatedContent(
        targetState = face,
        modifier = modifier,
        contentKey = { it.id },
        transitionSpec = {
            val fade = tween<Float>(motion.durationShort, easing = motion.easingStandard)
            fadeIn(fade).togetherWith(fadeOut(fade)).using(SizeTransform(clip = false))
        },
        label = "track",
    ) { content(it) }
}

/**
 * What is playing.
 *
 * Aligned to the start, not centred: a firm left edge anchors the column, and centred text under a
 * marquee reads as a placeholder.
 */
@Composable
private fun TrackInfo(title: String, artist: String) = Column {
    Text(
        text = title,
        style = VayouTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = VayouTheme.colors.onSurface,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee(),
    )
    Text(
        text = artist,
        style = VayouTheme.typography.bodyLarge,
        color = VayouTheme.colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VayouTheme.spacing.xxs),
    )
}

@Composable
private fun Progress(
    positionMs: Float,
    durationMs: Long,
    showTimes: Boolean,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
) {
    val span = durationMs.coerceAtLeast(1).toFloat()
    VayouSeekBar(
        fraction = positionMs / span,
        onSeekTo = { onSeek(it * span) },
        trackColor = VayouTheme.colors.onSurface.copy(alpha = TrackAlpha),
        activeColor = VayouTheme.colors.accent,
        thumbColor = VayouTheme.colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VayouTheme.spacing.lg),
        onScrub = { isScrubbing -> if (!isScrubbing) onSeekFinished() },
    )
    if (!showTimes) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = Utils.formatDurationMillis(positionMs.toLong()),
            style = VayouTheme.typography.labelMedium,
            color = VayouTheme.colors.onSurfaceVariant,
        )
        Text(
            text = Utils.formatDurationMillis(durationMs),
            style = VayouTheme.typography.labelMedium,
            color = VayouTheme.colors.onSurfaceVariant,
        )
    }
}

/**
 * Previous, play, next -- centred with a fixed gap rather than spread edge to edge, so the thumb
 * never leaves the arc it already travels for play.
 *
 * Shuffle and the sleep timer hold the two ends of the same line. Neither is transport: shuffle
 * decides what plays after this, the timer decides when it all stops, and neither belongs in the
 * arc a thumb sweeps between previous and next. Pinned to the edges by a box rather than spaced
 * into the row, so the three in the middle stay centred on the screen and not on what is left over.
 */
@Composable
private fun Transport(
    player: MediaController,
    isPlaying: Boolean,
    isShuffled: Boolean,
    isSleepTimerArmed: Boolean,
    onSleepTimer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ButtonRowPadding)
            .padding(top = VayouTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        ControlButton(
            onClick = { player.shuffleModeEnabled = !isShuffled },
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = VayouIcons.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = if (isShuffled) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                modifier = Modifier.size(VayouTheme.iconSize.md),
            )
        }
        ControlButton(onClick = onSleepTimer, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(
                imageVector = VayouIcons.Timer,
                contentDescription = stringResource(R.string.sleep_timer),
                // In the accent while it counts: this is the one control here that goes on doing
                // something after the screen is closed, and nothing else would say so.
                tint = if (isSleepTimerArmed) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                modifier = Modifier.size(VayouTheme.iconSize.md),
            )
        }
        // Sixteen inside the trio leaves about twenty-eight out to the two at the edges: near enough
        // to double that the three read as one control and the other two as neighbours.
        Row(
            horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(onClick = player::stepToPrevious) {
                Icon(
                    imageVector = VayouIcons.SkipPreviousFilled,
                    contentDescription = stringResource(R.string.previous),
                    tint = VayouTheme.colors.onSurface,
                    // A step above the row below: the drawn glyph keeps a margin the equalizer icon
                    // does not, so at the same token it renders visibly smaller.
                    modifier = Modifier.size(VayouTheme.iconSize.lg),
                )
            }
            // The one filled control on the screen. Everything else here is a glyph on the surface,
            // so play needs no label to be the thing a thumb goes to.
            Box(
                modifier = Modifier
                    .size(PlayerButtonSize.Primary)
                    .clip(CircleShape)
                    .background(VayouTheme.colors.onSurface)
                    .clickable { if (isPlaying) player.pause() else player.play() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) VayouIcons.PauseFilled else VayouIcons.Play,
                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    tint = VayouTheme.colors.surface,
                    modifier = Modifier.size(PlayGlyph),
                )
            }
            ControlButton(onClick = player::stepToNext) {
                Icon(
                    imageVector = VayouIcons.SkipNextFilled,
                    contentDescription = stringResource(R.string.next),
                    tint = VayouTheme.colors.onSurface,
                    modifier = Modifier.size(VayouTheme.iconSize.lg),
                )
            }
        }
    }
}

/**
 * What the player can open that does not act on the current track, so it sits apart from the
 * transport where nothing reaching for play can catch it.
 *
 * Drawn at full strength, like every other control here. A control is either offered or it is not;
 * dimmed, these read as disabled beside the transport.
 */
@Composable
private fun SecondaryActions(onEqualizer: () -> Unit, onQueue: () -> Unit, onLyrics: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ButtonRowPadding)
            .padding(top = VayouTheme.spacing.sm, bottom = VayouTheme.spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(onClick = onEqualizer) {
            Icon(
                imageVector = VayouIcons.Equalizer,
                contentDescription = stringResource(R.string.equalizer),
                tint = VayouTheme.colors.onSurface,
                modifier = Modifier.size(VayouTheme.iconSize.md),
            )
        }
        // In the middle, between the two ends this row has always had, and only for a track that
        // has words: drawn always it would be a key that does nothing on most of a library -- on
        // this one, where much of what plays is speech, on nearly all of it. The gap it leaves when
        // absent is the same gap the row had before there were words to show.
        onLyrics?.let { toggle ->
            ControlButton(onClick = toggle) {
                Icon(
                    imageVector = VayouIcons.Caption,
                    contentDescription = stringResource(R.string.lyrics),
                    tint = VayouTheme.colors.onSurface,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                )
            }
        }
        ControlButton(onClick = onQueue) {
            Icon(
                imageVector = VayouIcons.MusicPlaylist,
                contentDescription = stringResource(R.string.queue),
                tint = VayouTheme.colors.onSurface,
                modifier = Modifier.size(VayouTheme.iconSize.md),
            )
        }
    }
}

@Composable
private fun ControlButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(PlayerButtonSize.Secondary)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The seek bar.
 *
 * A canvas and not a Material slider: this one is drawn on a surface whose colour changes with the
 * cover, and the track has to stay visible against any of them.
 */
/**
 * Nearly the full width, short of the margin the text below it starts on.
 *
 * The square is the subject of this screen and the only thing on it worth looking at, so what it
 * gives up sideways it loses twice over in height. What it keeps is the column's own margin, which
 * is what stops the cover reading as a picture pasted over the screen rather than one laid on it.
 */
private const val CoverWidthFraction = 0.96f

private const val GradientMidpoint = 0.55f

private const val GradientMidBlend = 0.75f

private const val TickMs = 500L

private const val TrackAlpha = 0.3f

/**
 * What is read sits on this: the cover, the track, the seek bar and the times under it.
 *
 * Wider than a list's margin, because there is one thing on each line rather than a column of rows,
 * and a cover pinned to the edges reads as a wallpaper.
 */
private val ContentPadding = 20.dp

/**
 * What a row of round buttons sits on, and less than the content beside it.
 *
 * A round target carries a ring of slack of its own, so a button set flush with the text beside it
 * looks pushed inward by that ring; backing the row off by it puts the glyphs back on the column
 * the text is read on.
 */
private val ButtonRowPadding = 12.dp

private val WideGap = 32.dp

private val PlayGlyph = 32.dp

private val CastGlyphSize = 56.dp

/**
 * What is left over above the cover, against the one below it.
 *
 * Around a quarter above and three quarters below. Pinned to the top the square sat against the
 * bar; halfway down it drifted into the middle and the words under it lost their place. This is the
 * setting that keeps it clear of the chevron and still reads as the top half of the screen.
 */
private const val CoverHeadroom = 0.35f
