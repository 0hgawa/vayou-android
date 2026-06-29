package dev.vayou.feature.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import androidx.compose.material3.Icon
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.model.ControlButtonsPosition
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.player.PlayerViewModel
import dev.vayou.core.ui.R as coreUiR
import dev.vayou.core.ui.extensions.copy
import dev.vayou.feature.player.buttons.NextButton
import dev.vayou.feature.player.buttons.PlayPauseButton
import dev.vayou.feature.player.buttons.PlayerButton
import dev.vayou.feature.player.buttons.PreviousButton
import dev.vayou.feature.player.state.ControlsVisibilityState
import dev.vayou.feature.player.state.VerticalGesture
import dev.vayou.feature.player.state.rememberBrightnessState
import dev.vayou.feature.player.state.rememberControlsVisibilityState
import dev.vayou.feature.player.state.rememberErrorState
import dev.vayou.feature.player.state.rememberMediaPresentationState
import dev.vayou.feature.player.state.rememberMetadataState
import dev.vayou.feature.player.state.rememberPictureInPictureState
import dev.vayou.feature.player.state.rememberRotationState
import dev.vayou.feature.player.state.rememberSeekGestureState
import dev.vayou.feature.player.state.rememberTapGestureState
import dev.vayou.core.player.state.rememberVideoZoomAndContentScaleState
import dev.vayou.feature.player.state.rememberVolumeAndBrightnessGestureState
import dev.vayou.feature.player.state.rememberABRepeatState
import dev.vayou.feature.player.state.rememberPlaybackParametersState
import dev.vayou.core.player.state.rememberAmbientColor
import dev.vayou.feature.player.state.rememberNightModeState
import dev.vayou.feature.player.state.rememberSleepTimerState
import dev.vayou.feature.player.state.rememberVolumeState
import dev.vayou.feature.player.extensions.nameRes
import dev.vayou.core.player.extensions.subtitleDelayMilliseconds as savedSubtitleDelay
import dev.vayou.feature.player.state.seekAmountFormatted
import dev.vayou.feature.player.state.seekToPositionFormated
import dev.vayou.feature.player.ui.DoubleTapIndicator
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import dev.vayou.core.player.service.addSubtitleTrack
import dev.vayou.core.player.service.setSubtitleDelayMilliseconds
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleDelayMilliseconds
import dev.vayou.feature.player.state.rememberEqualizerState
import dev.vayou.feature.player.ui.OverlayShowView
import dev.vayou.feature.player.ui.OverlayView
import dev.vayou.core.player.ui.SubtitleConfiguration
import dev.vayou.core.player.model.SubtitleStyleState
import dev.vayou.feature.player.ui.VerticalProgressView
import dev.vayou.feature.player.ui.controls.ControlsBottomView
import dev.vayou.feature.player.ui.controls.ControlsTopView
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

val LocalControlsVisibilityState = compositionLocalOf<ControlsVisibilityState?> { null }
private const val TRANSLATION_LOOKAHEAD_MS = 2000L

@OptIn(UnstableApi::class)
@Composable
fun MediaPlayerScreen(
    player: Player?,
    viewModel: PlayerViewModel,
    playerPreferences: PlayerPreferences,
    modifier: Modifier = Modifier,
    showCastButton: Boolean = false,
    onSelectSubtitleClick: () -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
) {
    val volumeState = rememberVolumeState(
        player = player,
        showVolumePanelIfHeadsetIsOn = playerPreferences.showSystemVolumePanel,
    )
    player ?: return
    val playbackParametersState = rememberPlaybackParametersState(player)
    val metadataState = rememberMetadataState(player)
    val mediaPresentationState = rememberMediaPresentationState(player)
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        hideAfter = playerPreferences.controllerAutoHideTimeout.seconds,
    )
    val tapGestureState = rememberTapGestureState(
        player = player,
        doubleTapGesture = playerPreferences.doubleTapGesture,
        seekIncrementMillis = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        useLongPressGesture = playerPreferences.useLongPressControls,
        longPressSpeed = playerPreferences.longPressControlsSpeed,
    )
    val seekGestureState = rememberSeekGestureState(
        player = player,
        sensitivity = playerPreferences.seekSensitivity,
        enableSeekGesture = playerPreferences.useSeekControls,
    )
    val pictureInPictureState = rememberPictureInPictureState(
        player = player,
        autoEnter = playerPreferences.autoPip,
    )
    val videoZoomAndContentScaleState = rememberVideoZoomAndContentScaleState(
        player = player,
        initialContentScale = playerPreferences.playerVideoZoom,
        enableZoomGesture = playerPreferences.useZoomControls,
        enablePanGesture = playerPreferences.enablePanGesture,
        onEvent = viewModel::onVideoZoomEvent,
    )
    val brightnessState = rememberBrightnessState()
    val volumeAndBrightnessGestureState = rememberVolumeAndBrightnessGestureState(
        volumeState = volumeState,
        brightnessState = brightnessState,
        enableVolumeGesture = playerPreferences.enableVolumeSwipeGesture,
        enableBrightnessGesture = playerPreferences.enableBrightnessSwipeGesture,
        volumeGestureSensitivity = playerPreferences.volumeGestureSensitivity,
        brightnessGestureSensitivity = playerPreferences.brightnessGestureSensitivity,
    )
    val rotationState = rememberRotationState(
        player = player,
        screenOrientation = playerPreferences.playerScreenOrientation,
    )
    val errorState = rememberErrorState(player = player)

    LaunchedEffect(pictureInPictureState.isInPictureInPictureMode) {
        if (pictureInPictureState.isInPictureInPictureMode) {
            controlsVisibilityState.hideControls()
        }
    }

    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (tapGestureState.isLongPressGestureInAction) {
            controlsVisibilityState.hideControls()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (playerPreferences.rememberPlayerBrightness) {
            brightnessState.setBrightness(playerPreferences.playerBrightness)
        }
    }

    LaunchedEffect(brightnessState.currentBrightness) {
        if (playerPreferences.rememberPlayerBrightness) {
            viewModel.updatePlayerBrightness(brightnessState.currentBrightness)
        }
    }

    var surfaceView by remember { mutableStateOf<android.view.SurfaceView?>(null) }
    val ambientColor = rememberAmbientColor(player, surfaceView)

    val onlineSubtitleState by viewModel.onlineSubtitleState.collectAsState()
    val realtimeTranslationEnabled = playerPreferences.realtimeTranslationEnabled
    val realtimeTranslationLanguage = playerPreferences.realtimeTranslationLanguage

    LaunchedEffect(realtimeTranslationEnabled) {
        if (realtimeTranslationEnabled) {
            while (true) {
                val baseDelay = player.currentMediaItem?.mediaMetadata?.savedSubtitleDelay ?: 0L
                val target = baseDelay - TRANSLATION_LOOKAHEAD_MS
                when (player) {
                    is MediaController -> player.setSubtitleDelayMilliseconds(target)
                    is ExoPlayer -> player.subtitleDelayMilliseconds = target
                }
                delay(10_000)
            }
        } else {
            val baseDelay = player.currentMediaItem?.mediaMetadata?.savedSubtitleDelay ?: 0L
            when (player) {
                is MediaController -> player.setSubtitleDelayMilliseconds(baseDelay)
                is ExoPlayer -> player.subtitleDelayMilliseconds = baseDelay
            }
        }
    }

    var overlayView by remember { mutableStateOf<OverlayView?>(null) }
    val abRepeatState = rememberABRepeatState(player)
    var showAbRepeat by remember { mutableStateOf(false) }
    val sleepTimerState = rememberSleepTimerState(player)
    val equalizerState = rememberEqualizerState(
        player = player,
        controller = player as? MediaController,
        preferences = playerPreferences,
        onSave = viewModel::updatePlayerPreferences,
    )
    val nightModeState = rememberNightModeState(
        controller = player as? MediaController,
        preferences = playerPreferences,
        onSave = viewModel::updatePlayerPreferences,
    )

    CompositionLocalProvider(LocalControlsVisibilityState provides controlsVisibilityState) {
        Box {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(ambientColor.copy(alpha = 0.25f))
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.6f),
                            0.3f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.6f),
                        ),
                    ),
            ) {
                PlayerContentFrame(
                    player = player,
                    onSurfaceViewCreated = { surfaceView = it },
                    pictureInPictureState = pictureInPictureState,
                    controlsVisibilityState = controlsVisibilityState,
                    tapGestureState = tapGestureState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                    subtitleConfiguration = SubtitleConfiguration(
                        useSystemCaptionStyle = playerPreferences.useSystemCaptionStyle,
                        showBackground = playerPreferences.subtitleBackground,
                        font = playerPreferences.subtitleFont,
                        textSize = playerPreferences.subtitleTextSize,
                        textBold = playerPreferences.subtitleTextBold,
                        applyEmbeddedStyles = playerPreferences.applyEmbeddedStyles,
                        textColor = playerPreferences.subtitleTextColor,
                        shadow = playerPreferences.subtitleShadow,
                        outlineEnabled = playerPreferences.subtitleOutlineEnabled,
                        outlineColor = playerPreferences.subtitleOutlineColor,
                        verticalPosition = playerPreferences.subtitleVerticalPosition,
                        realtimeTranslationEnabled = realtimeTranslationEnabled,
                        realtimeTranslationLanguage = realtimeTranslationLanguage,
                        translationLookaheadMs = if (realtimeTranslationEnabled) TRANSLATION_LOOKAHEAD_MS else 0L,
                    ),
                )

                AnimatedVisibility(
                    visible = controlsVisibilityState.controlsVisible && !controlsVisibilityState.controlsLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                    )
                }

                if (mediaPresentationState.isBuffering) {
                    VayouCircularProgress(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp),
                        size = 72.dp,
                    )
                }

                DoubleTapIndicator(tapGestureState = tapGestureState)

                AnimatedVisibility(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.TopCenter),
                    visible = tapGestureState.isLongPressGestureInAction,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Surface(shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                        ) {
                            Text(
                                text = stringResource(coreUiR.string.fast_playback_speed, tapGestureState.longPressSpeed),
                                style = VayouTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                if (controlsVisibilityState.controlsVisible && controlsVisibilityState.controlsLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayerButton(
                            onClick = { controlsVisibilityState.unlockControls() },
                        ) {
                            Icon(
                                painter = painterResource(coreUiR.drawable.ic_lock),
                                contentDescription = stringResource(coreUiR.string.controls_unlock),
                            )
                        }
                    }
                } else {
                    PlayerControlsView(
                        topView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ControlsTopView(
                                    player = player,
                                    title = metadataState.title ?: "",
                                    showCastButton = showCastButton,
                                    isNightModeEnabled = nightModeState?.isEnabled ?: false,
                                    onNightModeToggle = { nightModeState?.toggle() },
                                    sleepTimerFormattedRemaining = sleepTimerState.formattedRemaining,
                                    onSleepTimerClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.SLEEP_TIMER
                                    },
                                    onEqualizerClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.EQUALIZER
                                    },
                                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                                    onBackClick = onBackClick,
                                )
                            }
                        },
                        middleView = {
                            when {
                                seekGestureState.seekAmount != null -> InfoView(info = "${seekGestureState.seekAmountFormatted}\n[${seekGestureState.seekToPositionFormated}]")
                                videoZoomAndContentScaleState.isZooming -> InfoView(info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%")
                                videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()))
                                controlsVisibilityState.controlsVisible -> ControlsMiddleView(player = player)
                                else -> Unit
                            }
                        },
                        bottomView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible && !controlsVisibilityState.controlsLocked,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                val context = LocalContext.current
                                ControlsBottomView(
                                    mediaPresentationState = mediaPresentationState,
                                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {
                                        ControlButtonsPosition.LEFT -> Alignment.Start
                                        ControlButtonsPosition.RIGHT -> Alignment.End
                                    },
                                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                                    isPipSupported = pictureInPictureState.isPipSupported,
                                    abRepeatState = abRepeatState,
                                    showAbRepeat = showAbRepeat,
                                    onToggleAbRepeat = { showAbRepeat = !showAbRepeat },
                                    onSeek = seekGestureState::onSeek,
                                    onSeekEnd = seekGestureState::onSeekEnd,
                                    onRotateClick = rotationState::rotate,
                                    onAudioClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.AUDIO_SELECTOR
                                    },
                                    onSubtitleClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.SUBTITLE_SELECTOR
                                    },
                                    playbackSpeed = playbackParametersState.speed,
                                    onPlaybackSpeedClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.PLAYBACK_SPEED
                                    },
                                    onPlaylistClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.PLAYLIST
                                    },
                                    onLockControlsClick = {
                                        controlsVisibilityState.showControls()
                                        controlsVisibilityState.lockControls()
                                    },
                                    onVideoContentScaleClick = {
                                        controlsVisibilityState.showControls()
                                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                                    },
                                    onVideoContentScaleLongClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.VIDEO_CONTENT_SCALE
                                    },
                                    onPictureInPictureClick = {
                                        if (!pictureInPictureState.hasPipPermission) {
                                            Toast.makeText(context, coreUiR.string.enable_pip_from_settings, Toast.LENGTH_SHORT).show()
                                            pictureInPictureState.openPictureInPictureSettings()
                                        } else {
                                            pictureInPictureState.enterPictureInPictureMode()
                                        }
                                    },
                                )
                            }
                        },
                    )
                }

                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .displayCutoutPadding()
                        .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
                        .padding(24.dp),
                ) {
                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterStart),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.VOLUME,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        VerticalProgressView(
                            value = volumeState.volumePercentage,
                            maxValue = volumeState.maxVolumePercentage,
                            icon = painterResource(coreUiR.drawable.ic_volume),
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.BRIGHTNESS,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        VerticalProgressView(
                            value = brightnessState.brightnessPercentage,
                            icon = painterResource(coreUiR.drawable.ic_brightness),
                        )
                    }
                }
            }

            OverlayShowView(
                player = player,
                overlayView = overlayView,
                videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                playerPreferences = playerPreferences,
                realtimeTranslationEnabled = realtimeTranslationEnabled,
                realtimeTranslationLanguage = realtimeTranslationLanguage,
                onlineSubtitleState = onlineSubtitleState,
                equalizerState = equalizerState,
                sleepTimerState = sleepTimerState,
                onDismiss = { overlayView = null },
                onSelectSubtitleClick = onSelectSubtitleClick,
                onOpenSubtitleStyle = { overlayView = OverlayView.SUBTITLE_STYLE },
                onOpenSubtitleSearchOnline = {
                    val mediaItem = player.currentMediaItem ?: return@OverlayShowView
                    viewModel.initOnlineSubtitleSearch(
                        videoUri = mediaItem.mediaId,
                        fileName = metadataState.title ?: mediaItem.mediaId.substringAfterLast("/"),
                    )
                    overlayView = OverlayView.SUBTITLE_SEARCH_ONLINE
                },
                onOpenTranslationLanguage = { overlayView = OverlayView.TRANSLATION_LANGUAGE },
                onBackToSubtitleSelector = { overlayView = OverlayView.SUBTITLE_SELECTOR },
                onRealtimeTranslationToggle = { viewModel.updatePlayerPreferences { p -> p.copy(realtimeTranslationEnabled = it) } },
                onRealtimeTranslationLanguageChange = { viewModel.updatePlayerPreferences { p -> p.copy(realtimeTranslationLanguage = it) } },
                onSubtitleOptionEvent = viewModel::onSubtitleOptionEvent,
                onSubtitleStyleChange = viewModel::updateSubtitleStyle,
                onVideoContentScaleChanged = { videoZoomAndContentScaleState.onVideoContentScaleChanged(it) },
                onOnlineSubtitleQueryChange = viewModel::updateOnlineSubtitleQuery,
                onOnlineSubtitleLanguageChange = viewModel::updateOnlineSubtitleLanguage,
                onOnlineSubtitleSearch = viewModel::searchSubtitlesOnline,
                onOnlineSubtitleResultClick = { result, index ->
                    viewModel.downloadOnlineSubtitle(result, index) { uri ->
                        (player as? MediaController)?.addSubtitleTrack(uri)
                        overlayView = null
                    }
                },
            )
        }
    }

    errorState.error?.let { error ->
        VayouDialog(
            onDismissRequest = { },
            title = { Text(text = stringResource(coreUiR.string.error_playing_video)) },
            content = { Text(text = error.message ?: stringResource(coreUiR.string.unknown_error)) },
            confirmButton = {
                if (player.hasNextMediaItem()) {
                    TextButton(
                        onClick = {
                            errorState.dismiss()
                            player.seekToNext()
                            player.play()
                        },
                    ) {
                        Text(text = stringResource(coreUiR.string.play_next_video))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        errorState.dismiss()
                        onBackClick()
                    },
                ) {
                    Text(text = stringResource(coreUiR.string.exit))
                }
            },
        )
    }

    BackHandler {
        if (overlayView != null) {
            overlayView = null
        } else {
            onBackClick()
        }
    }
}

@Composable
fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = VayouTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = info,
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ControlsMiddleView(modifier: Modifier = Modifier, player: Player) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviousButton(player = player)
        PlayPauseButton(player = player)
        NextButton(player = player)
    }
}

@Composable
fun PlayerControlsView(
    modifier: Modifier = Modifier,
    topView: @Composable () -> Unit,
    middleView: @Composable BoxScope.() -> Unit,
    bottomView: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            topView()
            Spacer(modifier = Modifier.weight(1f))
            bottomView()
        }

        middleView()
    }
}
