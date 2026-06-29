package dev.vayou.tv.feature.player

import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.view.SurfaceView
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.viewinterop.AndroidView
import dev.vayou.core.model.LoopMode
import dev.vayou.core.model.VideoContentScale
import dev.vayou.core.player.extensions.addAdditionalSubtitleConfiguration
import dev.vayou.core.player.extensions.subtitleDelayMilliseconds
import dev.vayou.core.player.extensions.uriToSubtitleConfiguration
import dev.vayou.core.common.extensions.getFilenameFromUri
import dev.vayou.core.player.service.PlayerService
import dev.vayou.core.player.service.setLoudnessGain
import dev.vayou.core.player.service.stopPlayerSession
import dev.vayou.core.player.service.setNightModeEnabled
import dev.vayou.core.player.service.setSubtitleDelayMilliseconds
import dev.vayou.core.player.state.rememberAmbientColor
import dev.vayou.core.player.ui.SubtitleConfiguration
import dev.vayou.core.player.ui.SubtitleView
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

private const val AutoHideMs = 4_000L
private const val ProgressRefreshMs = 500L
private const val TranslationLookaheadMs = 2_000L
private const val TranslationDelayRefreshMs = 10_000L

@OptIn(UnstableApi::class)
@Composable
fun TvPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: TvPlayerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val readyState by viewModel.ready.collectAsStateWithLifecycle()
    val preferences by viewModel.playerPreferences.collectAsStateWithLifecycle()

    val ready = readyState ?: run {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        )
        return
    }

    val initialPosition = ready.startPosition
    val playlist = ready.playlist

    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    LaunchedEffect(viewModel.videoUri) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlayerService::class.java),
        )
        val built = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .await()

        val mediaItems = playlist.map { video ->
            MediaItem.Builder()
                .setUri(video.uriString)
                .setMediaId(video.uriString)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(video.nameWithExtension).build())
                .build()
        }.ifEmpty {
            val title = context.getFilenameFromUri(android.net.Uri.parse(viewModel.videoUri))
            listOf(MediaItem.Builder().setUri(viewModel.videoUri).setMediaId(viewModel.videoUri).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()).build())
        }
        val startIndex = playlist.indexOfFirst { it.uriString == viewModel.videoUri }.coerceAtLeast(0)

        built.setMediaItems(mediaItems, startIndex, initialPosition)
        built.playWhenReady = true
        built.prepare()
        controller = built
    }

    DisposableEffect(Unit) {
        onDispose {
            val p = controller ?: return@onDispose
            viewModel.saveProgress(p.currentPosition, p.duration)
            // Tell PlayerService to stop the foreground media session, otherwise
            // the video keeps playing after we leave the player route.
            runCatching { p.stopPlayerSession() }
            p.release()
            controller = null
        }
    }

    val mediaController: MediaController = controller ?: run {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        )
        return
    }
    val player: Player = mediaController

    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    val ambientColor = rememberAmbientColor(player, surfaceView)

    var isPlaying by remember { mutableStateOf(player.isPlaying || player.playWhenReady) }
    var positionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0)) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(player.playbackParameters.speed) }
    var isLive by remember { mutableStateOf(player.isCurrentMediaItemLive) }
    var hasPrevious by remember { mutableStateOf(player.hasPreviousMediaItem()) }
    var hasNext by remember { mutableStateOf(player.hasNextMediaItem()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isPlaying = playWhenReady && player.playbackState != Player.STATE_IDLE
            }
            override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                playbackSpeed = params.speed
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                videoWidth = size.width
                videoHeight = size.height
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                isLive = player.isCurrentMediaItemLive
                hasPrevious = player.hasPreviousMediaItem()
                hasNext = player.hasNextMediaItem()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                isLive = player.isCurrentMediaItemLive
                hasPrevious = player.hasPreviousMediaItem()
                hasNext = player.hasNextMediaItem()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0)
            durationMs = player.duration.coerceAtLeast(0)
            delay(ProgressRefreshMs)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectorStack by remember { mutableStateOf<List<TvPlayerSelectorMode>>(emptyList()) }
    val selectorMode = selectorStack.lastOrNull()
    var contentScale by remember { mutableStateOf(VideoContentScale.BEST_FIT) }
    var loopMode by remember { mutableStateOf(LoopMode.OFF) }
    var sleepTimerDeadline by remember { mutableStateOf<Long?>(null) }
    var nightModeEnabled by remember { mutableStateOf(false) }
    var loudnessGainMb by remember { mutableIntStateOf(0) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(loopMode) {
        player.repeatMode = when (loopMode) {
            LoopMode.OFF -> Player.REPEAT_MODE_OFF
            LoopMode.ONE -> Player.REPEAT_MODE_ONE
            LoopMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    LaunchedEffect(preferences.realtimeTranslationEnabled) {
        fun savedDelayMs(): Long =
            mediaController.currentMediaItem?.mediaMetadata?.subtitleDelayMilliseconds ?: 0L
        if (preferences.realtimeTranslationEnabled) {
            while (true) {
                mediaController.setSubtitleDelayMilliseconds(savedDelayMs() - TranslationLookaheadMs)
                delay(TranslationDelayRefreshMs)
            }
        } else {
            mediaController.setSubtitleDelayMilliseconds(savedDelayMs())
        }
    }

    LaunchedEffect(sleepTimerDeadline) {
        val deadline = sleepTimerDeadline ?: return@LaunchedEffect
        val remaining = deadline - System.currentTimeMillis()
        if (remaining > 0) delay(remaining)
        if (sleepTimerDeadline == deadline) {
            player.pause()
            sleepTimerDeadline = null
        }
    }

    fun poke() {
        controlsVisible = true
        lastInteractionMs = System.currentTimeMillis()
    }

    fun openSelector(target: TvPlayerSelectorMode) {
        poke()
        selectorStack = listOf(target)
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            controlsVisible = true
            lastInteractionMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(controlsVisible, lastInteractionMs, selectorMode, isPlaying) {
        if (!controlsVisible || selectorMode != null || !isPlaying) return@LaunchedEffect
        delay(AutoHideMs)
        val idleFor = System.currentTimeMillis() - lastInteractionMs
        if (idleFor >= AutoHideMs) controlsVisible = false
    }

    val scope = rememberCoroutineScope()
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val config = context.uriToSubtitleConfiguration(uri = uri, isSelected = true)
            player.addAdditionalSubtitleConfiguration(config)
        }
    }

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    BackHandler {
        if (selectorMode != null) selectorStack = selectorStack.dropLast(1) else onBack()
    }

    val compact = selectorMode != null
    val videoEndPadding by animateDpAsState(
        targetValue = if (compact) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "video-end-padding",
    )
    val videoPaddingVertical by animateDpAsState(
        targetValue = if (compact) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "video-padding-v",
    )
    val videoStartPadding by animateDpAsState(
        targetValue = if (compact) 32.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "video-start-padding",
    )
    val panelWeight by animateFloatAsState(
        targetValue = if (compact) 0.42f else 0.0001f,
        animationSpec = tween(durationMillis = 250),
        label = "panel-weight",
    )
    val videoWeight by animateFloatAsState(
        targetValue = if (compact) 0.58f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "video-weight",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .background(ambientColor.copy(alpha = 0.25f))
            .background(
                brush = Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.6f),
                    0.3f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.6f),
                ),
            )
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) poke()
                false
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(videoWeight)
                    .padding(
                        start = videoStartPadding,
                        end = videoEndPadding,
                        top = videoPaddingVertical,
                        bottom = videoPaddingVertical,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val ratio = remember(videoWidth, videoHeight) {
                    if (videoWidth > 0 && videoHeight > 0) {
                        videoWidth.toFloat() / videoHeight.toFloat()
                    } else 16f / 9f
                }
                val surfaceModifier = when (contentScale) {
                    VideoContentScale.STRETCH -> Modifier.fillMaxSize()
                    VideoContentScale.CROP -> Modifier.fillMaxSize().aspectRatio(ratio, matchHeightConstraintsFirst = true)
                    VideoContentScale.BEST_FIT,
                    VideoContentScale.HUNDRED_PERCENT -> Modifier.aspectRatio(ratio)
                }
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).also { sv ->
                            player.setVideoSurfaceView(sv)
                            surfaceView = sv
                        }
                    },
                    modifier = surfaceModifier,
                )
                DisposableEffect(player) {
                    onDispose { player.clearVideoSurface() }
                }
                SubtitleView(
                    player = player,
                    configuration = SubtitleConfiguration(
                        useSystemCaptionStyle = preferences.useSystemCaptionStyle,
                        showBackground = preferences.subtitleBackground,
                        font = preferences.subtitleFont,
                        textSize = preferences.subtitleTextSize,
                        textBold = preferences.subtitleTextBold,
                        applyEmbeddedStyles = preferences.applyEmbeddedStyles,
                        textColor = preferences.subtitleTextColor,
                        shadow = preferences.subtitleShadow,
                        outlineEnabled = preferences.subtitleOutlineEnabled,
                        outlineColor = preferences.subtitleOutlineColor,
                        verticalPosition = preferences.subtitleVerticalPosition,
                        realtimeTranslationEnabled = preferences.realtimeTranslationEnabled,
                        realtimeTranslationLanguage = preferences.realtimeTranslationLanguage,
                        translationLookaheadMs = if (preferences.realtimeTranslationEnabled) TranslationLookaheadMs else 0L,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(panelWeight)
                    .padding(
                        end = if (compact) 32.dp else 0.dp,
                        top = 24.dp,
                        bottom = 24.dp,
                    ),
            ) {
                selectorMode?.let { mode ->
                    TvPlayerSelectorOverlay(
                        mode = mode,
                        player = player,
                        isLive = isLive,
                        currentContentScale = contentScale,
                        currentLoopMode = loopMode,
                        currentSleepTimerDeadline = sleepTimerDeadline,
                        currentNightMode = nightModeEnabled,
                        currentLoudnessGainMb = loudnessGainMb,
                        currentSubtitleDelayMs = subtitleDelayMs,
                        currentTranslationEnabled = preferences.realtimeTranslationEnabled,
                        currentTranslationLanguage = preferences.realtimeTranslationLanguage,
                        currentPreferences = preferences,
                        onSubtitleStyleUpdate = { transform ->
                            viewModel.updatePreferences(transform)
                        },
                        onContentScaleChange = { contentScale = it },
                        onLoopModeChange = { loopMode = it },
                        onSleepTimerChange = { minutes ->
                            sleepTimerDeadline = minutes?.let { System.currentTimeMillis() + it * 60_000L }
                        },
                        onNightModeChange = { enabled ->
                            nightModeEnabled = enabled
                            mediaController.setNightModeEnabled(enabled)
                        },
                        onLoudnessGainChange = { gain ->
                            loudnessGainMb = gain
                            mediaController.setLoudnessGain(gain)
                        },
                        onSubtitleDelayChange = { delay ->
                            subtitleDelayMs = delay
                            mediaController.setSubtitleDelayMilliseconds(delay)
                        },
                        onTranslationEnabledChange = { enabled ->
                            viewModel.updatePreferences { it.copy(realtimeTranslationEnabled = enabled) }
                        },
                        onTranslationLanguageChange = { code ->
                            viewModel.updatePreferences { it.copy(realtimeTranslationLanguage = code) }
                        },
                        onAddExternalSubtitle = {
                            subtitlePicker.launch(arrayOf("*/*"))
                        },
                        onSwitchMode = { selectorStack = selectorStack + it },
                        onDismiss = { selectorStack = selectorStack.dropLast(1) },
                    )
                }
            }
        }

        if (!compact) {
            TvPlayerControls(
                visible = controlsVisible,
                isPlaying = isPlaying,
                isLive = isLive,
                positionMs = positionMs,
                durationMs = durationMs,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                onPlayPause = {
                    poke()
                    if (player.isPlaying) player.pause() else player.play()
                },
                onPrevious = {
                    poke()
                    if (player.hasPreviousMediaItem()) player.seekToPrevious()
                },
                onNext = {
                    poke()
                    if (player.hasNextMediaItem()) player.seekToNext()
                },
                onSeek = { ms ->
                    poke()
                    val max = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                    player.seekTo(ms.coerceIn(0L, max))
                },
                onAudioTracks = { openSelector(TvPlayerSelectorMode.Audio) },
                onSubtitleTracks = { openSelector(TvPlayerSelectorMode.Subtitle) },
                onSpeed = { openSelector(TvPlayerSelectorMode.Speed) },
                onPlaylist = { openSelector(TvPlayerSelectorMode.Playlist) },
                onMore = { openSelector(TvPlayerSelectorMode.More) },
                playbackSpeed = playbackSpeed,
            )
        }
    }
}

enum class TvPlayerSelectorMode {
    Audio, Subtitle, SubtitleTracks, Speed,
    Aspect, Loop, Timer,
    NightMode, Loudness, SubtitleDelay,
    Translation,
    Playlist, More,
    SubtitleStyle, SubtitleStyleCustom,
    SubtitleSize, SubtitlePosition, SubtitleFont, SubtitleTextColor,
    SubtitleOutline, SubtitleOutlineColor,
    SubtitleShadow, SubtitleBold, SubtitleBackground,
}
