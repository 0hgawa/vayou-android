package dev.vayou.feature.player

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberNextButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.state.rememberPreviousButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.vayou.core.model.DoubleTapGesture
import dev.vayou.core.player.SubtitleMimeTypes
import dev.vayou.core.player.stepToNext
import dev.vayou.core.player.stepToPrevious
import dev.vayou.core.player.ui.SubtitleOverlay
import dev.vayou.core.player.ui.VideoContentScale
import dev.vayou.core.player.ui.rememberTracksState
import dev.vayou.core.ui.designsystem.components.VayouSheetRow
import dev.vayou.core.ui.designsystem.components.VayouSwitch
import dev.vayou.core.ui.theme.VayouTheme
import kotlin.math.sign
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
internal fun PlayerScreen(
    request: PlaybackRequest,
    isInPictureInPicture: Boolean,
    onBack: () -> Unit,
    onPlayerReady: (Player) -> Unit,
    onPlayInBackground: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(request, viewModel.controller) { viewModel.open(request) }

    // Nothing until the service answers. The window is already black, so there is nothing to draw
    // in the meantime that is not a flash of something else.
    val player = viewModel.controller ?: return
    LaunchedEffect(player) { onPlayerReady(player) }

    // Nothing on this screen is about a picture once there is no picture on it. Branched here rather
    // than dressed around: the surface, the gestures, the framing and the brightness all act on a
    // pane that is no longer showing anything.
    rememberSelectedRoute()?.let { route ->
        CastScreen(player = player, deviceName = route.name, onBack = onBack)
        return
    }

    val activity = requireNotNull(LocalActivity.current) { "PlayerScreen outside an activity" }
    KeepScreenOnWhilePlaying(player = player, activity = activity)
    // Read once, on the first composition: the state owns the level from then on, and feeding it
    // back what it just wrote would fight the drag it is in the middle of. Held in a remember for
    // that reason -- read in place it was taken again on every recomposition, which is a read of
    // the stored settings on each frame of a drag for a value that is only ever used on the first.
    val rememberedBrightness = remember {
        val preferences = viewModel.preferences.value
        preferences.playerBrightness.takeIf { preferences.rememberPlayerBrightness }
    }
    val brightness = rememberBrightnessState(
        activity = activity,
        remembered = rememberedBrightness,
        onChanged = { level -> viewModel.updatePreferences { copy(playerBrightness = level) } },
    )
    val volume = rememberVolumeState(LocalContext.current)
    val progress = rememberProgressStateWithTickInterval(player = player, tickIntervalMs = TickIntervalMs)
    val presentation = rememberPresentationState(player)
    val zoomState = rememberZoomState()
    val abRepeat = rememberABRepeatState(player)
    val sleepTimer = rememberSleepTimerState(player)
    var sleepSheetOpen by remember { mutableStateOf(false) }
    var queueSheetOpen by remember { mutableStateOf(false) }
    var equalizerSheetOpen by remember { mutableStateOf(false) }
    var onlineSheetOpen by remember { mutableStateOf(false) }
    // Null is the file's own words, which is what a subtitle is for until it is not enough.
    var translateTo: String? by remember { mutableStateOf(null) }
    var styleSheetOpen by remember { mutableStateOf(false) }
    var languageSheetOpen by remember { mutableStateOf(false) }
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val equalizer = rememberEqualizerState(
        player = player,
        preferences = preferences,
        onSave = viewModel::updatePreferences,
    )
    val pickSubtitle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        picked?.let(viewModel::addSubtitle)
    }
    val density = LocalDensity.current

    val controllerTimeout = preferences.controllerAutoHideTimeout.seconds
    val skipIncrementMs = preferences.seekIncrement.seconds.inWholeMilliseconds
    val brightnessFactor = preferences.brightnessGestureSensitivity.asFactor()
    val volumeFactor = preferences.volumeGestureSensitivity.asFactor()

    // One question, once the service is there: whether this device amplifies at all. Until it
    // answers, the gesture runs to the device's own maximum and no further.
    LaunchedEffect(player, preferences.enableVolumeBoost) {
        volume.bind(player, preferences.enableVolumeBoost)
    }

    var controlsVisible by remember { mutableStateOf(true) }
    // Bumped by anything that counts as still being here. The countdown below is keyed on it, and
    // restarting a LaunchedEffect is what cancels the wait it was already in.
    var lastInteraction by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    // Left out of the composition entirely when it is off, so there is no listener, no loop and no
    // copy off the surface -- not a switch checked inside something that runs anyway.
    val ambient = if (preferences.useAmbientGlow) {
        rememberAmbientGlow(player = player, window = activity.window)
    } else {
        Color.Black
    }
    var axis: GestureAxis? by remember { mutableStateOf(null) }
    var seekTargetMs by remember { mutableLongStateOf(0L) }
    // Non-null only while a finger is dragging the seek bar, holding where it has reached.
    var scrubbingToMs: Long? by remember { mutableStateOf(null) }
    var skipMs by remember { mutableLongStateOf(0L) }
    var openPicker: PickerKind? by remember { mutableStateOf(null) }
    var scale by remember { mutableStateOf(VideoContentScale.BestFit) }
    // Non-null only while the pill is naming the framing that was just chosen.
    var announcedScale: VideoContentScale? by remember { mutableStateOf(null) }
    val isLive = rememberIsLive(player, hint = request.isLive)
    val hasArrived = rememberHasArrived(player, request.uri)

    val centreReadout = playerCentreReadout(
        axis = axis,
        scale = announcedScale,
        skipMs = skipMs,
        seekTargetMs = seekTargetMs,
        scrubbingToMs = scrubbingToMs,
    )
    // The renderer has to know before the cues do: translating asks for every line early, and the
    // offset is applied to the track rather than to what is drawn from it.
    LaunchedEffect(translateTo) { viewModel.setTranslating(translateTo != null) }
    val subtitleTracks = rememberTracksState(player, C.TRACK_TYPE_TEXT)
    val audioTracks = rememberTracksState(player, C.TRACK_TYPE_AUDIO)
    val speed = rememberPlaybackSpeedState(player)
    val skipSilence = rememberSkipSilenceState(player)
    val playPause = rememberPlayPauseButtonState(player)
    val errorState = rememberErrorState(player)

    /** Puts the controls up and starts their clock over. */
    val showControls: () -> Unit = {
        controlsVisible = true
        lastInteraction++
    }

    // The bars follow the controls, so that what the controls measure themselves against is what
    // is actually on screen. Locked, they stay away: the point of locking is a bare picture.
    LaunchedEffect(controlsVisible, isLocked) {
        (activity as? PlayerActivity)?.showSystemBars(controlsVisible && !isLocked)
    }

    /**
     * Something is open, or a finger is on something.
     *
     * The clock does not run while this is true, and starts over the moment the last of them goes.
     * Taking the controls out from under a hand that is using them is the one thing an auto-hide
     * must never do -- and it was doing it for every sheet but the pickers, and for a drag on the
     * bar that outlasted the timeout.
     */
    val isEngaged = openPicker != null ||
        sleepSheetOpen ||
        queueSheetOpen ||
        equalizerSheetOpen ||
        onlineSheetOpen ||
        styleSheetOpen ||
        languageSheetOpen ||
        scrubbingToMs != null

    // Only while the film is running. Paused, the controls are the only thing on screen worth
    // looking at, and taking them away would leave a still frame and no way to say what to do
    // with it.
    LaunchedEffect(controlsVisible, lastInteraction, playPause.showPlay, isEngaged, controllerTimeout) {
        if (!controlsVisible || playPause.showPlay || isEngaged) return@LaunchedEffect
        delay(controllerTimeout)
        controlsVisible = false
    }

    LaunchedEffect(announcedScale) {
        if (announcedScale == null) return@LaunchedEffect
        delay(ScaleReadoutLingerMs)
        announcedScale = null
    }

    // Every skip restarts this, so a run of them keeps one total on screen and the total clears
    // once the taps stop. Restarting a LaunchedEffect cancels the delay it was already in, which is
    // the whole of the behaviour -- no job to hold and cancel by hand.
    LaunchedEffect(skipMs) {
        if (skipMs != 0L) {
            delay(SkipReadoutLingerMs)
            skipMs = 0L
        }
    }

    val skip: (forward: Boolean) -> Unit = { forward ->
        val step = skipIncrementMs * if (forward) 1L else -1L
        // Only the floor is ours to hold. Asking for a position past the end is how you reach the
        // end, and the player already stops there.
        player.seekTo((player.currentPosition + step).coerceAtLeast(0L))
        // A change of direction starts a new run, so back-then-forward reads +10 rather than 0.
        skipMs = if (skipMs.sign == step.sign) skipMs + step else step
        lastInteraction++
    }

    CompositionLocalProvider(LocalPlayerDiscs provides preferences.useMaterialYouControls) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VayouTheme.colors.videoBackdrop)
                // Over the backdrop rather than instead of it: the field is a quarter-strength
                // wash, and what it washes has to be the black a letterbox is, not whatever is
                // behind the window. Nothing at all until a frame has been sampled, so a film that
                // fills the screen never pays for it.
                //
                // Stretched from four cells to the whole screen, and the bilinear filter that does
                // the stretching is what makes it a wash rather than four squares.
                .drawBehind {
                    // Black is the backdrop already: painting it again is a full-screen fill for
                    // nothing, and this is the state the setting leaves behind when it is off.
                    if (ambient == Color.Black) return@drawBehind
                    drawRect(color = ambient)
                    // Faded back to black away from the picture, so what is left is a glow hugging
                    // the film rather than a slab of colour with a hard edge against the frame. A
                    // flat wash reads as a second, duller picture on a bright scene, and the eye
                    // goes to the join. Under the film, which is opaque, so only the bars see it.
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            GlowStart to Color.Transparent,
                            GlowEnd to Color.Transparent,
                            1f to Color.Black,
                        ),
                    )
                }
                .playerGestures(
                    enabled = !isLocked,
                    // Nowhere to drag to: a channel is at its live edge, and the only position
                    // it has is now.
                    isSeekEnabled = preferences.useSeekControls && !isLive,
                    isBrightnessEnabled = preferences.enableBrightnessSwipeGesture,
                    isVolumeEnabled = preferences.enableVolumeSwipeGesture,
                    isZoomEnabled = preferences.useZoomControls,
                    onTap = { if (controlsVisible) controlsVisible = false else showControls() },
                    onDoubleTap = { fraction ->
                        val action = doubleTapActionAt(fraction, preferences.doubleTapGesture)
                            // Skipping is seeking by another name, so it goes where seeking goes.
                            // Play/pause stays: pausing a channel is how you answer the door.
                            ?.takeUnless { isLive && it != DoubleTapAction.PlayPause }
                        // Undo what the first of the two taps did. A double tap is not a request to
                        // change whether the controls are up, and leaving them flipped makes every one
                        // of them cost a third tap to put the screen back.
                        if (action != null) controlsVisible = !controlsVisible
                        when (action) {
                            DoubleTapAction.Back -> skip(false)
                            DoubleTapAction.Forward -> skip(true)
                            DoubleTapAction.PlayPause -> playPause.onClick()
                            null -> Unit
                        }
                    },
                    onStart = { started ->
                        axis = started
                        // Where the seek starts from, so the whole drag is measured against one point
                        // rather than accumulating rounding on every frame.
                        if (started == GestureAxis.Seek) seekTargetMs = progress.currentPositionMs
                    },
                    onDrag = { dragged, delta ->
                        when (dragged) {
                            GestureAxis.Brightness -> brightness.nudge(delta * brightnessFactor)
                            GestureAxis.Volume -> volume.nudge(delta * volumeFactor)
                            GestureAxis.Seek -> {
                                val span = progress.durationMs.takeIf { it > 0 } ?: return@playerGestures
                                val reach = SeekReach * preferences.seekSensitivity.asFactor()
                                seekTargetMs = (seekTargetMs + delta * span * reach).toLong().coerceIn(0L, span)
                            }
                        }
                    },
                    onPinchStart = { showControls() },
                    onPinch = zoomState::pinch,
                    onPinchEnd = { viewModel.saveZoom(zoomState.zoom) },
                    onEnd = {
                        // The seek lands once, on release. Seeking on every frame of the drag asks the
                        // decoder for a keyframe dozens of times and the picture never settles.
                        if (axis == GestureAxis.Seek) player.seekTo(seekTargetMs)
                        axis = null
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // The conversion is not the redundancy it looks like. Media3's videoSizeDp is the video's
            // own pixels with its pixel aspect ratio already applied, despite the name, and the modifier
            // wants dp. Three of the four scales only read the ratio and would not notice, but a hundred
            // percent is a FixedScale(1f): there the number *is* the size, so dropping this would show a
            // 1080p film at 1080 physical pixels instead of the 1080dp that a hundred percent means.
            val sourceSizeDp = presentation.videoSizeDp?.let { size ->
                with(density) { size.copy(width = size.width.toDp().value, height = size.height.toDp().value) }
            }
            val framing = Modifier.resizeWithContentScale(scale.toContentScale(), sourceSizeDp)
            // A layer and not a size: pinching redraws the same surface bigger, where remeasuring it
            // would ask the decoder for a new output size on every frame of the gesture. It grows about
            // its own centre, which is the default origin and the only one that makes sense here.
            val pinched = framing.graphicsLayer {
                scaleX = zoomState.zoom
                scaleY = zoomState.zoom
            }

            PlayerSurface(player = player, modifier = pinched)

            // Above the frame and below the controls: a caption must not sit under the scrim, and the
            // controls must not sit under a caption. Framed with the picture, so a caption follows the
            // edge of the film rather than the edge of the screen.
            SubtitleOverlay(player = player, modifier = pinched, translateTo = translateTo, style = preferences)

            // Covered until the frame is ready *and* its shape is known.
            //
            // Media3 lifts its own shutter when the first frame renders, but the modifier that gives
            // the surface its aspect takes the size from a different callback -- and with no size it
            // does not resize at all, so the surface is the whole box. Waiting on the first frame
            // alone shows a frame or two of the film stretched edge to edge before it snaps to its
            // own shape, which is the flash on opening.
            //
            // Black, not a scrim: this is the absence of a picture rather than something laid over
            // one, and it sits above the captions so none are drawn on an empty screen.
            if (!hasArrived || presentation.coverSurface || presentation.videoSizeDp == null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black),
                )
            }

            // Enabled at the ends too, because there the button turns the queue over rather than
            // doing nothing -- see [stepToNext].
            //
            // The count is read rather than watched: the two states below are recomposed by the
            // player's own listener on the events that change it, and all that is asked of it is
            // whether there is a queue at all, which does not change under a queue that grows.
            val previousStep = rememberPreviousButtonState(player)
            val nextStep = rememberNextButtonState(player)
            val isQueued = player.mediaItemCount > 1

            // Nothing over the frame in the floating window: it is a thumbnail, the system draws its
            // own button on it, and a scrim would leave nothing of the film to see.
            PlayerControls(
                playPause = playPause,
                canGoPrevious = previousStep.isEnabled || isQueued,
                canGoNext = nextStep.isEnabled || isQueued,
                onPrevious = player::stepToPrevious,
                onNext = player::stepToNext,
                speed = speed,
                title = rememberTitleState(player).title,
                onBack = onBack,
                positionMs = progress.currentPositionMs,
                durationMs = progress.durationMs,
                onSeek = { positionMs ->
                    // Only while dragging: a tap on the bar is a seek, not a question about where
                    // it would land, and it should not blink the transport away for a frame.
                    if (scrubbingToMs != null) scrubbingToMs = positionMs
                    player.seekTo(positionMs)
                },
                onScrub = { isScrubbing ->
                    scrubbingToMs = if (isScrubbing) progress.currentPositionMs else null
                    viewModel.setScrubbing(isScrubbing)
                },
                onRotate = activity::toggleOrientation,
                onOpenSleepTimer = { sleepSheetOpen = true },
                isSleepTimerArmed = sleepTimer.isArmed,
                onOpenEqualizer = { equalizerSheetOpen = true },
                // False where the device has no equalizer at all, which is the same as off.
                isEqualizerOn = equalizer?.isEnabled == true,
                isNightMode = preferences.nightModeEnabled,
                onToggleNightMode = { viewModel.setNightMode(!preferences.nightModeEnabled) },
                onPlayInBackground = onPlayInBackground,
                onEnterPictureInPicture = onEnterPictureInPicture.takeIf { PictureInPicture.isSupported },
                isLive = isLive,
                hasTextTracks = subtitleTracks.tracks.isNotEmpty(),
                hasAudioChoice = audioTracks.tracks.size > 1,
                abRepeat = abRepeat,
                onCycleScale = {
                    scale = scale.next
                    // Framing and zoom would otherwise compound into a size nobody asked for.
                    zoomState.reset()
                    announcedScale = scale
                    lastInteraction++
                },
                onLock = {
                    isLocked = true
                    // Clear the frame in the same breath. Locking is a request to be left with the
                    // film, and leaving the way out on screen answers it with something still on top.
                    controlsVisible = false
                },
                onOpenScales = { openPicker = PickerKind.Scale },
                onOpenSubtitles = { openPicker = PickerKind.Subtitle },
                onOpenAudioTracks = { openPicker = PickerKind.Audio },
                onOpenQueue = { queueSheetOpen = true },
                onOpenSpeed = { openPicker = PickerKind.Speed },
                visible = controlsVisible && !isInPictureInPicture && !isLocked,
                // The middle is the readout's while one is showing: two things cannot share a
                // centre, and what the picture is doing outranks the keys that cannot be used
                // while it does it.
                showTransport = centreReadout == null,
                // Any touch on the controls is the viewer still being here, so the clock starts
                // over. Watched rather than taken: the buttons underneath still get the press, and
                // without this a row of taps on the framing button ran out the timeout set by the
                // first of them.
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            // Presses only. This counter is a key of the countdown below, so
                            // bumping it on every move would cancel and relaunch that coroutine
                            // once per frame of a drag -- and a drag already holds the clock
                            // through [isEngaged].
                            if (event.type == PointerEventType.Press) lastInteraction++
                        }
                    }
                },
            )

            // Takes the place of the controls rather than sitting alongside them, and answers the same
            // tap: locked or not, touching the film is how the viewer asks for what they can press.
            UnlockButton(
                visible = controlsVisible && !isInPictureInPicture && isLocked,
                onUnlock = { isLocked = false },
                modifier = Modifier.align(Alignment.Center),
            )

            val options = when (openPicker) {
                PickerKind.Subtitle -> null
                // No "off" for sound: a file playing with its audio disabled is a fault, not a choice.
                PickerKind.Audio -> trackOptions(audioTracks, offLabel = null)
                PickerKind.Speed -> speedOptions(speed)
                PickerKind.Scale -> VideoContentScale.entries.map { option ->
                    PlayerOption(
                        label = stringResource(option.label),
                        isSelected = option == scale,
                        onSelect = {
                            scale = option
                            zoomState.reset()
                            openPicker = null
                        },
                    )
                }

                null -> null
            }
            if (options != null) {
                PlayerOptionsSheet(
                    title = stringResource(
                        when (openPicker) {
                            PickerKind.Subtitle -> R.string.subtitles
                            PickerKind.Audio -> R.string.audio_track
                            PickerKind.Scale -> R.string.video_framing
                            PickerKind.Speed, null -> R.string.playback_speed
                        },
                    ),
                    options = options,
                    onDismiss = { openPicker = null },
                    // Beside the speed and nowhere else: both are about how long the thing takes,
                    // and on a lecture the silences are the larger of the two savings.
                    footer = if (openPicker == PickerKind.Speed) {
                        {
                            VayouSheetRow(
                                text = stringResource(R.string.skip_silence),
                                onClick = skipSilence::toggle,
                                trailing = {
                                    VayouSwitch(
                                        checked = skipSilence.isEnabled,
                                        onCheckedChange = { skipSilence.toggle() },
                                    )
                                },
                            )
                        }
                    } else {
                        null
                    },
                )
            }

            if (openPicker == PickerKind.Subtitle) {
                SubtitleSheet(
                    tracks = subtitleTracks.tracks,
                    isOff = subtitleTracks.isOff,
                    onSelectTrack = subtitleTracks::select,
                    onTurnOff = subtitleTracks::turnOff,
                    isLive = isLive,
                    onOpenFile = { pickSubtitle.launch(SubtitleMimeTypes) },
                    onSearchOnline = {
                        openPicker = null
                        onlineSheetOpen = true
                    },
                    onCustomise = {
                        openPicker = null
                        styleSheetOpen = true
                    },
                    translateTo = translateTo,
                    // Portuguese on first turning it on, since that is the language of whoever is most
                    // likely to want a translation of what they are watching.
                    onTranslateToggle = { on -> translateTo = if (on) DefaultTranslationLanguage else null },
                    onPickLanguage = {
                        openPicker = null
                        languageSheetOpen = true
                    },
                    delayMs = viewModel.subtitleDelayMs,
                    onDelayChange = viewModel::setSubtitleDelay,
                    onDismiss = { openPicker = null },
                )
            }

            if (languageSheetOpen) {
                PlayerOptionsSheet(
                    title = stringResource(R.string.translate_subtitle),
                    options = TranslationLanguages.map { language ->
                        PlayerOption(
                            label = language.label,
                            isSelected = translateTo == language.code,
                            onSelect = { translateTo = language.code },
                        )
                    },
                    onDismiss = { languageSheetOpen = false },
                )
            }

            if (styleSheetOpen) {
                SubtitleStyleSheet(
                    style = preferences,
                    onChange = viewModel::setSubtitleStyle,
                    onDismiss = { styleSheetOpen = false },
                )
            }

            if (onlineSheetOpen) {
                OnlineSubtitleSheet(
                    state = viewModel.onlineSubtitles,
                    onSearch = { query, language -> viewModel.searchSubtitles(query, language) },
                    // Back to the track list once the file is on the film, because that list is
                    // the answer: the caption is already selected by the time this runs, and the
                    // search results said nothing about it. A viewer left staring at the same ten
                    // rows has no way to tell a download that worked from one that did nothing.
                    onPick = { result ->
                        viewModel.downloadSubtitle(result) {
                            onlineSheetOpen = false
                            openPicker = PickerKind.Subtitle
                        }
                    },
                    onDismiss = { onlineSheetOpen = false },
                )
            }

            if (equalizerSheetOpen) {
                // Null on a build without a session behind the player, which cannot happen here and
                // which the state does not promise away.
                equalizer?.let { EqualizerSheet(state = it, onDismiss = { equalizerSheetOpen = false }) }
            }

            if (queueSheetOpen) {
                PlayerQueueSheet(player = player, onDismiss = { queueSheetOpen = false })
            }

            if (sleepSheetOpen) {
                SleepTimerSheet(state = sleepTimer, onDismiss = { sleepSheetOpen = false })
            }

            errorState.error?.let { error ->
                PlayerErrorDialog(error = error, onRetry = errorState::retry, onLeave = onBack)
            }

            PlayerReadoutPill(
                // Under the title bar, not across the middle of the frame. The top is where the
                // chrome lives; a readout in the centre lands on the film itself, over the face of
                // whoever is talking.
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = ReadoutTopInset),
                readout = playerReadout(
                    axis = axis,
                    brightness = brightness.value,
                    volumePercent = volume.percent,
                ).takeUnless { isInPictureInPicture },
            )

            // In the middle, where the transport is -- and instead of it. Outside the controls
            // rather than inside them, because a drag along the film happens with the controls
            // down and the readout still has to be seen.
            PlayerCentreReadout(
                text = centreReadout.takeUnless { isInPictureInPicture },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/**
 * Holds the screen awake while a film is running.
 *
 * Nothing else does. The phone's idle timeout counts touches, and watching is the one thing a
 * viewer does without touching anything -- so a film watched through dims partway and then sleeps.
 *
 * Tied to playback rather than to the screen being open: a film left paused has no claim on the
 * backlight, and neither has one whose activity is going away.
 */
@Composable
private fun KeepScreenOnWhilePlaying(player: Player, activity: Activity) {
    DisposableEffect(player, activity) {
        val window = activity.window

        // Asked to play, rather than currently playing.
        //
        // `isPlaying` is false while the player buffers, and a live channel buffers constantly --
        // every stall would drop the flag, and enough of them in a row let the phone dim over a
        // picture the viewer is waiting on. What the viewer decided is that this should be playing,
        // and the decision does not lapse because the network hiccuped.
        //
        // Still false when the file ends: a finished film left on its last frame is not being
        // watched, and the phone may sleep as it would have anyway.
        fun sync() {
            val isWatching = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            if (isWatching) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = sync()

            override fun onPlaybackStateChanged(playbackState: Int) = sync()
        }
        // Once up front: this screen is composed after the film has already started.
        sync()
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Twice a second. A seek bar crossing a phone's width moves less than a pixel between ticks on
 *  anything longer than a few minutes, and asking more often only wakes the main thread. */
private const val TickIntervalMs = 500L

/** Clear of the title bar above it, so the readout does not read as part of the controls. */
/**
 * Where the glow has faded out, top and bottom, as a share of the screen.
 *
 * Just inside the widest film a phone shows, so the colour is at full strength where it meets the
 * picture and gone by the edge of the screen. Nothing is lost behind the film itself: this is drawn
 * under it, and the film is opaque.
 */
private const val GlowStart = 0.28f

private const val GlowEnd = 0.72f

private val ReadoutTopInset = 84.dp

/**
 * A drag across the whole screen covers a third of the file, at the middle sensitivity.
 *
 * One-to-one is unusable on anything feature length -- a finger's width would be a minute, and no
 * one can land on a scene that way.
 */
private const val SeekReach = 0.33f

/**
 * A stored sensitivity as a multiplier.
 *
 * The figure on disk runs from nothing to one with the default in the middle, which is how the
 * slider reads it. What a gesture wants is a factor either side of one, so a viewer who leaves the
 * slider alone gets exactly what the app did before the setting existed.
 */
private fun Float.asFactor(): Float = this * 2f

/** What a double tap does where it landed, or null for nothing at all. */
private enum class DoubleTapAction { Back, Forward, PlayPause }

private fun doubleTapActionAt(fraction: Float, gesture: DoubleTapGesture): DoubleTapAction? = when (gesture) {
    DoubleTapGesture.NONE -> null
    DoubleTapGesture.PLAY_PAUSE -> DoubleTapAction.PlayPause
    DoubleTapGesture.SEEK -> if (fraction < Half) DoubleTapAction.Back else DoubleTapAction.Forward
    // Play and pause in the middle third, seeking at the edges: two targets a thumb can tell apart
    // without looking, on a screen whose middle is where the film is.
    DoubleTapGesture.BOTH -> when {
        fraction < MiddleThirdStart -> DoubleTapAction.Back
        fraction > MiddleThirdEnd -> DoubleTapAction.Forward
        else -> DoubleTapAction.PlayPause
    }
}

private const val Half = 0.5f

private const val MiddleThirdStart = 1f / 3f

private const val MiddleThirdEnd = 2f / 3f

/** One second, which is how long the name of a framing takes to read and no longer. */
private const val ScaleReadoutLingerMs = 1_000L

/** Long enough to read the total after the last tap of a run, short enough not to sit over the film. */
private const val SkipReadoutLingerMs = 750L

/** Which list the one dialog on screen is showing. */
private enum class PickerKind { Subtitle, Audio, Speed, Scale }

/**
 * Whether what is playing is a live stream.
 *
 * Read through a listener rather than straight off the player: it is a fact about the item in the
 * timeline, and the timeline arrives after the screen does. Read once during composition it would
 * be false for every channel, because nothing has loaded yet at that point.
 */
/**
 * Whether the player has reached the film this screen was opened for.
 *
 * The service outlives the screen, so opening a second film attaches a new surface to a player that
 * is still on the first one -- and it draws it. For the third of a second it takes to read the
 * folder, look the file up and prepare it, the screen shows the *previous* film, then cuts. That is
 * the flash on opening, and no amount of asking the presentation state catches it: the surface is
 * not covered and the video size is not unknown, because both still belong to the film going out.
 *
 * Latched rather than compared on every frame. Once the queue is running, "next" legitimately moves
 * the current item away from the one this screen named, and the shutter must not come back down
 * between two films of the same folder.
 */
@Composable
private fun rememberHasArrived(player: Player, uri: String): Boolean {
    var arrived by remember(player, uri) { mutableStateOf(player.currentMediaItem?.mediaId == uri) }
    DisposableEffect(player, uri, arrived) {
        if (arrived) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem?.mediaId == uri) arrived = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return arrived
}

@Composable
private fun rememberIsLive(player: Player, hint: Boolean): Boolean {
    var isLive by remember(player) { mutableStateOf(hint || player.isCurrentMediaItemLive) }
    DisposableEffect(player) {
        // Only once the window is real. Setting the queue produces a timeline at once, but its
        // windows are placeholders -- they answer "not live" for everything, because nothing has
        // been fetched yet. Asked then, a channel is called a film for as long as it takes the
        // manifest to arrive, which is the several seconds the viewer is looking at the screen.
        fun settle() {
            val timeline = player.currentTimeline
            if (timeline.isEmpty) return
            val index = player.currentMediaItemIndex
            if (index !in 0 until timeline.windowCount) return
            if (timeline.getWindow(index, Timeline.Window()).isPlaceholder) return
            // The timeline may promote a stream to live; it never demotes one the caller said was
            // live. Only the channel list sends that word, and a channel is live whatever the
            // stream turns out to look like -- plenty are plain transport streams with no manifest
            // to say so, and reading the window would dress a channel as a film a second in. The
            // television has always trusted the caller for the whole session; this is that.
            isLive = hint || player.isCurrentMediaItemLive
        }

        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) = settle()

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = settle()
        }
        settle()
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return isLive
}

/** What each way of fitting a film is called on this phone. */
internal val VideoContentScale.label: Int
    get() = when (this) {
        VideoContentScale.BestFit -> R.string.scale_best_fit
        VideoContentScale.Stretch -> R.string.scale_stretch
        VideoContentScale.Crop -> R.string.scale_crop
        VideoContentScale.HundredPercent -> R.string.scale_hundred_percent
    }
