package dev.vayou.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.ui.compose.state.PlayPauseButtonState
import androidx.media3.ui.compose.state.PlaybackSpeedState
import dev.vayou.core.player.isNightModeSupported
import dev.vayou.core.player.ui.asSpeedLabel
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenuItem
import dev.vayou.core.ui.theme.VayouTheme

/**
 * What is on the frame while the controls are up.
 *
 * Three bands, as the app has always had them: what is playing across the top, the transport in the
 * middle where a thumb reaches without covering the picture, and everything you might change along
 * the bottom.
 *
 * White on a scrim rather than palette colours, for the reason the surface behind gives: this lies
 * on a frame whose brightness nobody chose, so it carries its own contrast.
 */
@Composable
fun PlayerControls(
    playPause: PlayPauseButtonState,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    speed: PlaybackSpeedState,
    /** Null until the player has read the file's metadata, which is a moment after the first frame. */
    title: String?,
    onBack: () -> Unit,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onScrub: (Boolean) -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit,
    onCycleScale: () -> Unit,
    onOpenScales: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    isSleepTimerArmed: Boolean,
    onOpenEqualizer: () -> Unit,
    isNightMode: Boolean,
    onToggleNightMode: () -> Unit,
    onPlayInBackground: () -> Unit,
    onEnterPictureInPicture: (() -> Unit)?,
    /**
     * A live channel, which most of this bar has nothing to say about.
     *
     * There is no length to seek within, no stretch to mark out and repeat, no folder behind it to
     * queue, and no speed to play the present at. Those four go, rather than sitting there greyed:
     * a control that is never usable on this kind of media is not a disabled control, it is one
     * that does not belong on the screen.
     */
    isLive: Boolean,
    /** Whether the stream carries captions of its own -- live has no other way to get any. */
    hasTextTracks: Boolean,
    /** Whether there is more than one language to choose between. One is not a choice. */
    hasAudioChoice: Boolean,
    abRepeat: ABRepeatState,
    onOpenSubtitles: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSpeed: () -> Unit,
    visible: Boolean,
    /** False while the middle is saying what the picture is doing. See [PlayerCentreReadout]. */
    showTransport: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VayouTheme.colors.videoScrim),
        ) {
            TopBar(
                title = title,
                onBack = onBack,
                onOpenSleepTimer = onOpenSleepTimer,
                isSleepTimerArmed = isSleepTimerArmed,
                onOpenEqualizer = onOpenEqualizer,
                isNightMode = isNightMode,
                onToggleNightMode = onToggleNightMode,
                onPlayInBackground = onPlayInBackground,
                modifier = Modifier.align(Alignment.TopStart),
            )

            if (showTransport) {
                Transport(
                    playPause = playPause,
                    canGoPrevious = canGoPrevious,
                    canGoNext = canGoNext,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            BottomBar(
                speed = speed,
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = onSeek,
                onScrub = onScrub,
                onLock = onLock,
                onRotate = onRotate,
                onCycleScale = onCycleScale,
                onOpenScales = onOpenScales,
                onEnterPictureInPicture = onEnterPictureInPicture,
                isLive = isLive,
                hasTextTracks = hasTextTracks,
                hasAudioChoice = hasAudioChoice,
                abRepeat = abRepeat,
                onOpenSubtitles = onOpenSubtitles,
                onOpenAudioTracks = onOpenAudioTracks,
                onOpenQueue = onOpenQueue,
                onOpenSpeed = onOpenSpeed,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String?,
    onBack: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    isSleepTimerArmed: Boolean,
    onOpenEqualizer: () -> Unit,
    isNightMode: Boolean,
    onToggleNightMode: () -> Unit,
    onPlayInBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = SideInset, vertical = BarInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TitleGap),
    ) {
        PlayerButton(onClick = onBack) {
            Icon(
                imageVector = VayouIcons.ArrowBack,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(PlayerButtonSize.StandardGlyph),
            )
        }

        // One line that travels, rather than two lines or an ellipsis. This bar floats over the
        // picture, so a second line would move the controls beside it the moment a file has a long
        // name; and what an ellipsis eats is the end -- "1080p BluRay x265" -- which is the part of
        // a file name worth reading.
        Text(
            text = title.orEmpty(),
            style = VayouTheme.typography.titleMedium,
            color = VayouTheme.colors.onVideo,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(initialDelayMillis = MarqueeDelayMs),
        )

        CastButton(onVideo = true)

        Box {
            PlayerButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = VayouIcons.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    modifier = Modifier.size(PlayerButtonSize.StandardGlyph),
                )
            }
            VayouDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                VayouDropdownMenuItem(
                    text = stringResource(R.string.sleep_timer),
                    icon = VayouIcons.Timer,
                    // Lit while one is running, so the menu says so without being opened twice.
                    contentColor = if (isSleepTimerArmed) {
                        VayouTheme.colors.accent
                    } else {
                        VayouTheme.colors.onSurface
                    },
                    onClick = {
                        menuOpen = false
                        onOpenSleepTimer()
                    },
                )
                VayouDropdownMenuItem(
                    text = stringResource(R.string.equalizer),
                    icon = VayouIcons.Equalizer,
                    onClick = {
                        menuOpen = false
                        onOpenEqualizer()
                    },
                )
                if (isNightModeSupported) {
                    VayouDropdownMenuItem(
                        text = stringResource(
                            if (isNightMode) R.string.night_mode_on else R.string.night_mode,
                        ),
                        icon = VayouIcons.DarkMode,
                        contentColor = if (isNightMode) {
                            VayouTheme.colors.accent
                        } else {
                            VayouTheme.colors.onSurface
                        },
                        // The menu stays up: this is a setting whose effect is heard rather than
                        // seen, and closing on it would leave nothing saying it took.
                        onClick = onToggleNightMode,
                    )
                }
                VayouDropdownMenuItem(
                    text = stringResource(R.string.background_play),
                    icon = VayouIcons.Background,
                    onClick = {
                        menuOpen = false
                        onPlayInBackground()
                    },
                )
            }
        }
    }
}

@Composable
private fun Transport(
    playPause: PlayPauseButtonState,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TransportGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dimmed at the ends of the queue, never taken away. Removing one moves play out from the
        // middle of the row and shifts the other under a thumb that was already reaching for it --
        // a control that is not where it was a second ago is worse than one that says no.
        TransportButton(
            glyph = VayouIcons.SkipPreviousFilled,
            label = R.string.previous_file,
            enabled = canGoPrevious,
            onClick = onPrevious,
        )

        PlayerButton(
            onClick = playPause::onClick,
            size = PlayerButtonSize.Primary,
        ) {
            Icon(
                imageVector = if (playPause.showPlay) VayouIcons.Play else VayouIcons.PauseFilled,
                contentDescription = stringResource(if (playPause.showPlay) R.string.play else R.string.pause),
                modifier = Modifier.size(PlayerButtonSize.PrimaryGlyph),
            )
        }

        TransportButton(
            glyph = VayouIcons.SkipNextFilled,
            label = R.string.next_file,
            enabled = canGoNext,
            onClick = onNext,
        )
    }
}

@Composable
private fun BottomBar(
    speed: PlaybackSpeedState,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onScrub: (Boolean) -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit,
    onCycleScale: () -> Unit,
    onOpenScales: () -> Unit,
    onEnterPictureInPicture: (() -> Unit)?,
    isLive: Boolean,
    hasTextTracks: Boolean,
    hasAudioChoice: Boolean,
    abRepeat: ABRepeatState,
    onOpenSubtitles: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSpeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Read from the window rather than from a Configuration: the same answer without building one,
    // and without the deprecation that comes with asking for it.
    val containerSize = LocalWindowInfo.current.containerSize
    val isPortrait = containerSize.height >= containerSize.width

    Column(
        modifier = modifier
            .safeDrawingPadding()
            .padding(horizontal = SideInset, vertical = BarInset),
        verticalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        // A live stream has no duration and no position to read against one, so the clock and the
        // bar are replaced rather than shown empty -- a seek bar with nowhere to go is a control
        // that lies about what it can do.
        if (isLive) {
            LiveMark()
        } else {
            // The clock and the bar are one block, on the margin the music player gives its own:
            // a seek bar is dragged to its ends, and a thumb that stops half a finger from the edge
            // of the screen is one you cannot put at zero. The discs below keep the narrower inset,
            // as they do there -- a round target already carries its own air.
            Column(modifier = Modifier.padding(horizontal = SeekInset)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Elapsed(positionMs = positionMs, durationMs = durationMs)
                    RepeatMarks(abRepeat)
                }

                PlayerSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = onSeek,
                    onScrub = onScrub,
                    repeatFromMs = abRepeat.pointA,
                    repeatToMs = abRepeat.pointB,
                )
            }
        }

        // Two rows in one: what is on the left travels, and the queue on the right does not.
        //
        // Portrait folds everything about *how the film is shown* behind one button, because eight
        // discs across a phone is a row that scrolls -- and a control you have to scroll to find is
        // one you stop using. Landscape has the width, so it shows them.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ButtonGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // On a file the button opens the way in to a subtitle -- a track, a file on this
                // phone, a search. A channel has only what it broadcasts, so with nothing broadcast
                // there is nothing behind the button.
                if (!isLive || hasTextTracks) {
                    BarButton(VayouIcons.Caption, R.string.subtitles, onOpenSubtitles)
                }
                if (!isLive || hasAudioChoice) {
                    BarButton(VayouIcons.Audio, R.string.audio_track, onOpenAudioTracks)
                }
                if (!isLive) {
                    // The number itself, not a glyph of a dial. It is the one control whose current
                    // value is worth reading from across the room.
                    PlayerButton(onClick = onOpenSpeed) {
                        Text(text = speed.playbackSpeed.asSpeedLabel(), style = VayouTheme.typography.titleSmall)
                    }
                }

                if (isPortrait) {
                    FramingMenu(
                        onLock = onLock,
                        onRotate = onRotate,
                        onOpenScales = onOpenScales,
                        abRepeat = abRepeat,
                        isLive = isLive,
                        onEnterPictureInPicture = onEnterPictureInPicture,
                    )
                } else {
                    BarButton(VayouIcons.Lock, R.string.lock, onLock)
                    BarButton(VayouIcons.Rotation, R.string.rotate, onRotate)
                    // Tap to step through the four, hold for the list. Cycling is quicker when the
                    // next one is the one wanted, and worse than useless when it is the one just
                    // passed.
                    BarButton(VayouIcons.Size, R.string.video_framing, onCycleScale, onLongClick = onOpenScales)
                    if (!isLive) BarButton(VayouIcons.Section, R.string.repeat_section, abRepeat::toggleA)
                    onEnterPictureInPicture?.let { BarButton(VayouIcons.Pip, R.string.picture_in_picture, it) }
                }
            }

            // Outside the travelling row, so it keeps the corner. What is playing next is a
            // different question from how this one is shown, and it is the one control here that
            // must never be scrolled off.
            if (!isLive) BarButton(VayouIcons.Queue, R.string.queue, onOpenQueue)
        }
    }
}

/**
 * Everything about how the film is shown, behind one button.
 *
 * Only in portrait. These five are the ones a viewer sets once and leaves -- how it is framed, which
 * way up, whether it is locked -- so they lose nothing by being a tap further away, and the three
 * that are used mid-film keep the width.
 */
@Composable
private fun FramingMenu(
    onLock: () -> Unit,
    onRotate: () -> Unit,
    onOpenScales: () -> Unit,
    abRepeat: ABRepeatState,
    isLive: Boolean,
    onEnterPictureInPicture: (() -> Unit)?,
) {
    var isOpen by remember { mutableStateOf(false) }
    Box {
        PlayerButton(onClick = { isOpen = true }) {
            Icon(
                imageVector = VayouIcons.MoreHoriz,
                contentDescription = stringResource(R.string.more_options),
                modifier = Modifier.size(PlayerButtonSize.StandardGlyph),
            )
        }
        VayouDropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            VayouDropdownMenuItem(
                text = stringResource(R.string.lock),
                icon = VayouIcons.Lock,
                onClick = {
                    isOpen = false
                    onLock()
                },
            )
            VayouDropdownMenuItem(
                text = stringResource(R.string.rotate),
                icon = VayouIcons.Rotation,
                onClick = {
                    isOpen = false
                    onRotate()
                },
            )
            // The list, not the cycle. A menu row cannot be held, and stepping blind through four
            // framings from inside a menu is worse than being shown them.
            VayouDropdownMenuItem(
                text = stringResource(R.string.video_framing),
                icon = VayouIcons.Size,
                onClick = {
                    isOpen = false
                    onOpenScales()
                },
            )
            if (!isLive) {
                VayouDropdownMenuItem(
                    text = stringResource(R.string.repeat_section),
                    icon = VayouIcons.Section,
                    onClick = {
                        isOpen = false
                        abRepeat.toggleA()
                    },
                    // Lit while a stretch is marked out, so the menu says so without being opened
                    // twice -- the same rule the sleep timer follows in the bar above.
                    contentColor = if (abRepeat.pointA != null) {
                        VayouTheme.colors.accent
                    } else {
                        VayouTheme.colors.onSurface
                    },
                )
            }
            onEnterPictureInPicture?.let { enterPip ->
                VayouDropdownMenuItem(
                    text = stringResource(R.string.picture_in_picture),
                    icon = VayouIcons.Pip,
                    onClick = {
                        isOpen = false
                        enterPip()
                    },
                )
            }
        }
    }
}

/**
 * The two marks, beside the clock, and only once one has been set.
 *
 * Beside the time and not among the buttons below: while a stretch is being marked out, when each
 * mark falls is the only thing that matters, and the numbers belong next to the number.
 */
@Composable
private fun RepeatMarks(abRepeat: ABRepeatState) {
    val a = abRepeat.pointA ?: return
    Row(
        modifier = Modifier.padding(start = MarksGap),
        horizontalArrangement = Arrangement.spacedBy(MarksGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.repeat_from, formatTime(a)),
            style = VayouTheme.typography.labelMedium,
            color = VayouTheme.colors.accentFixed,
        )
        Text(
            text = abRepeat.pointB?.let { stringResource(R.string.repeat_to, formatTime(it)) }
                ?: stringResource(R.string.repeat_to_unset),
            style = VayouTheme.typography.labelMedium,
            color = if (abRepeat.isLooping) VayouTheme.colors.accentFixed else VayouTheme.colors.onVideo,
            modifier = Modifier
                .clickable(interactionSource = null, indication = null, onClick = abRepeat::toggleB)
                .padding(horizontal = TextInset),
        )
    }
}

/** Tap to swap the elapsed time for what is left, which is the question during a long film. */
@Composable
private fun Elapsed(positionMs: Long, durationMs: Long) {
    var showRemaining by remember { mutableStateOf(false) }
    val elapsed = if (showRemaining) {
        "-${formatTime((durationMs - positionMs).coerceAtLeast(0L))}"
    } else {
        formatTime(positionMs)
    }

    Text(
        text = "$elapsed / ${formatTime(durationMs)}",
        style = VayouTheme.typography.bodyMedium,
        color = VayouTheme.colors.onVideo,
        modifier = Modifier
            .clickable(interactionSource = null, indication = null) { showRemaining = !showRemaining }
            .padding(vertical = TextInset),
    )
}

@Composable
private fun TransportButton(
    glyph: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    // The smaller disc, as on the music player: at the same size as play all three read as one row
    // of equals, and the thing a thumb goes to first has to be the biggest.
    PlayerButton(onClick = onClick, size = PlayerButtonSize.Secondary, enabled = enabled) {
        Icon(
            imageVector = glyph,
            contentDescription = stringResource(label),
            modifier = Modifier.size(PlayerButtonSize.StandardGlyph),
        )
    }
}

/** A dot and a word where the clock would be: what is playing has no end to count towards. */
@Composable
private fun LiveMark() {
    Row(
        modifier = Modifier.padding(horizontal = TextInset, vertical = MarksGap),
        horizontalArrangement = Arrangement.spacedBy(TextInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LiveDotSize)
                // The fixed amber, as everything else that lands on a frame: how bright the picture
                // is has nothing to do with which theme the app is in.
                .background(VayouTheme.colors.accentFixed, CircleShape),
        )
        Text(
            text = stringResource(R.string.player_live),
            style = VayouTheme.typography.labelLarge,
            color = VayouTheme.colors.onVideo,
        )
    }
}

@Composable
private fun BarButton(
    glyph: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    PlayerButton(onClick = onClick, onLongClick = onLongClick) {
        Icon(
            imageVector = glyph,
            contentDescription = stringResource(label),
            modifier = Modifier.size(PlayerButtonSize.StandardGlyph),
        )
    }
}

/** Enough to lift a white glyph off a bright frame, little enough to keep watching through it. */
/** Long enough to read the start of a name before it begins to travel. */
private const val MarqueeDelayMs = 1_200

private val SideInset = 8.dp

/**
 * What the clock and the bar sit on, over and above [SideInset].
 *
 * Twenty in total, which is the margin the music player's own progress bar keeps. The two are the
 * same control on two screens, and the film's was eight from the edge -- close enough that the thumb
 * at either end was half off the screen.
 */
private val SeekInset = 12.dp

private val BarInset = 16.dp

private val TextInset = 8.dp

private val TitleGap = 16.dp

private val TransportGap = 40.dp

private val ButtonGap = 8.dp

private val RowGap = 4.dp

private val MarksGap = 8.dp

private val LiveDotSize = 8.dp
