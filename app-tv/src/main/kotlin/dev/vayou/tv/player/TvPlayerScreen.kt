package dev.vayou.tv.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.data.models.OnlineSubtitleState
import dev.vayou.core.data.models.OpenSubtitleResult
import dev.vayou.core.data.models.SubtitleLanguages
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.player.MaxVolumeBoostMillibels
import dev.vayou.core.player.NoVolumeBoost
import dev.vayou.core.player.PlaybackCommands
import dev.vayou.core.player.PlaybackService
import dev.vayou.core.player.SubtitleMimeTypes
import dev.vayou.core.player.addSubtitle
import dev.vayou.core.player.externalSubtitle
import dev.vayou.core.player.isVolumeBoostSupported
import dev.vayou.core.player.setNightMode
import dev.vayou.core.player.setSleepTimer
import dev.vayou.core.player.setSubtitleDelay
import dev.vayou.core.player.setVolumeBoost
import dev.vayou.core.player.stepToNext
import dev.vayou.core.player.stepToPrevious
import dev.vayou.core.player.ui.SubtitleOverlay
import dev.vayou.core.player.ui.SubtitlePreset
import dev.vayou.core.player.ui.SubtitleSizePreset
import dev.vayou.core.player.ui.TracksState
import dev.vayou.core.player.ui.TranslationLookaheadMs
import dev.vayou.core.player.ui.VideoContentScale
import dev.vayou.core.player.ui.asSpeedLabel
import dev.vayou.core.player.ui.rememberTracksState
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.tv.Hairline
import dev.vayou.tv.R
import dev.vayou.tv.SplitMs
import dev.vayou.tv.TvAction
import dev.vayou.tv.TvActions
import dev.vayou.tv.TvCardTitleGap
import dev.vayou.tv.TvDialog
import dev.vayou.tv.TvDialogWidth
import dev.vayou.tv.TvMessage
import dev.vayou.tv.TvRowGap
import dev.vayou.tv.TvRowInset
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvTextField
import dev.vayou.tv.TvTickMs
import dev.vayou.tv.TvTitleInset
import dev.vayou.tv.WholeScreen
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * A film on the television.
 *
 * Nothing here is touched, so nothing here is a target: the whole screen takes the focus and reads
 * the remote directly. A control that has to be focused before it can be pressed costs a viewer
 * four presses to pause, which is why a television player looks like a bar and behaves like a
 * keypad.
 */
@Composable
fun TvPlayerScreen(onBack: () -> Unit, viewModel: TvPlayerViewModel = hiltViewModel()) {
    val opening by viewModel.opening.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var connected by remember { mutableStateOf<MediaController?>(null) }

    // Started the moment the screen opens, and not once the library has answered. Binding to the
    // service and asking the database where the film was left are both waits, and they are waits
    // for different things: run one after the other and a channel -- which the library knows
    // nothing about -- still pays for the question.
    LaunchedEffect(Unit) {
        val token = SessionToken(context.applicationContext, ComponentName(context, PlaybackService::class.java))
        connected = MediaController.Builder(context.applicationContext, token).buildAsync().await()
    }

    // Both answers go into the same call: setting the items and then seeking would start the film
    // at the beginning and jump.
    LaunchedEffect(opening, connected) {
        val start = opening ?: return@LaunchedEffect
        val connected = connected ?: return@LaunchedEffect

        val items = start.queue
            .map { video -> mediaItem(video.uriString, video.displayName) }
            // A film that is in no library -- one on a share, or a channel -- is named by the end of
            // its own address, decoded, or a space in the name would read as `%20`.
            .ifEmpty { listOf(mediaItem(viewModel.videoUri, Uri.decode(viewModel.videoUri.substringAfterLast('/')))) }
        connected.setMediaItems(
            items,
            items.indexOfFirst { it.mediaId == viewModel.videoUri }.coerceAtLeast(0),
            start.startPosition,
        )
        connected.prepare()
        connected.play()
        controller = connected
    }

    val player = controller

    // Paused when the screen stops, and let go of only when it goes away for good.
    //
    // Two moments, and the first one is the whole of the fix. A composition is not disposed when a
    // television puts its own launcher in front of the app -- the activity is stopped, not
    // destroyed -- so a film left in that state went on playing, with sound, invisibly, for as long
    // as the app was alive. Opening something else then showed the new film's name over the old
    // film's audio.
    //
    // Paused and not stopped, because stopping is for leaving: a viewer who pressed home in the
    // middle of a film and came back should find it where they left it.
    LifecycleStartEffect(Unit) { onStopOrDispose { connected?.pause() } }

    // Read off `connected` rather than off the film that was queued from it: backgrounding while
    // the first frame is still being waited for used to leak the controller entirely, because the
    // only teardown was hung off a value that had not been set yet.
    DisposableEffect(Unit) {
        onDispose {
            val active = connected ?: return@onDispose
            viewModel.saveProgress(active.currentPosition, active.duration)
            // Stopped rather than left running: leaving the screen on a television is leaving the
            // film, and there is no notification here to bring it back from.
            active.stop()
            active.release()
        }
    }

    // A share goes off the network, a codec meets something it cannot decode, and the player stops
    // with a black screen and no explanation -- which reads as the app having frozen. Held here
    // rather than inside `Playing` so that a failure on the very first frame is caught too.
    var failure by remember { mutableStateOf<PlaybackException?>(null) }
    DisposableEffect(connected) {
        val active = connected ?: return@DisposableEffect onDispose {}
        // The `Changed` form and not `onPlayerError`, because it also fires with null: preparing
        // again clears the error, and that is what takes this screen back to the film.
        val listener = object : Player.Listener {
            override fun onPlayerErrorChanged(error: PlaybackException?) {
                failure = error
            }
        }
        active.addListener(listener)
        onDispose { active.removeListener(listener) }
    }

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val failed = failure
        when {
            failed != null -> PlaybackFailed(
                failure = failed,
                onRetry = {
                    connected?.prepare()
                    connected?.play()
                },
                onBack = onBack,
            )

            // Something, rather than a black screen. Binding to the service and reaching the first
            // frame of a live stream is a second or two on a good night, and a viewer given nothing
            // to look at reads that as the app having failed rather than as it working.
            player == null -> Text(
                text = stringResource(R.string.opening),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )

            else -> Playing(
                controller = player,
                player = player,
                preferences = preferences,
                isKnownLive = viewModel.isLive,
                onStyle = viewModel::updatePreferences,
                onlineSubtitles = viewModel.onlineSubtitles,
                subtitleLanguage = viewModel.subtitleLanguage,
                subtitleQuery = viewModel.subtitleQuery,
                onSearchSubtitles = viewModel::searchSubtitles,
                onDownloadSubtitle = { result ->
                    viewModel.downloadSubtitle(result) { file ->
                        scope.launch { player.addSubtitle(context.externalSubtitle(file)) }
                    }
                },
                onChooseSubtitleLanguage = viewModel::chooseSubtitleLanguage,
                onSearchSubtitlesFor = viewModel::searchSubtitlesFor,
                onBack = onBack,
            )
        }
    }
}

/**
 * What is playing, where the picture would be.
 *
 * A mark and a name, and nothing else -- there is no artwork to show. A track on a share is a file
 * on a disc somewhere, and reading its cover out of the tags is a second round trip over the
 * network for something the viewer is listening to rather than looking at.
 */
@Composable
private fun NowPlaying(title: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SleeveMark),
        )
        Spacer(modifier = Modifier.height(TvTitleInset))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = TvScreenInset),
        )
    }
}

private val SleeveMark = 72.dp

/**
 * What went wrong, and the two things worth doing about it.
 *
 * Recoverable on purpose: the commonest failure here is a share that was reachable a minute ago and
 * will be again in another one, and leaving the viewer to walk back through four folders to find
 * that out is the app punishing them for its own network.
 */
@Composable
private fun PlaybackFailed(failure: PlaybackException, onRetry: () -> Unit, onBack: () -> Unit) {
    val retry = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { retry.requestFocus() } }
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier.width(TvDialogWidth),
        verticalArrangement = Arrangement.spacedBy(TvRowGap),
    ) {
        Text(
            text = stringResource(R.string.playback_failed),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = TvRowInset),
        )
        Text(
            text = stringResource(failure.explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = TvRowInset, vertical = TvCardTitleGap),
        )
        // The way out first and the way on second, as everywhere else -- but the focus starts on
        // the second, because whoever reached this screen came here to watch something.
        TvActions {
            TvAction(stringResource(R.string.go_back), onClick = onBack)
            TvAction(stringResource(R.string.try_again), Modifier.focusRequester(retry), onRetry)
        }
    }
}

/**
 * Which of two sentences to show: the network's fault, or the file's.
 *
 * Two and not the exception's own message, which is a class name and a byte offset. The distinction
 * is the one that decides what a viewer does next -- try again, or try something else. Read off the
 * range rather than off a list of codes: everything the player counts as getting at the bytes is
 * numbered in the two thousands, and everything about making sense of them starts at the three.
 */
private val PlaybackException.explanation: Int
    get() = if (errorCode in IoErrorCodes) R.string.playback_failed_network else R.string.playback_failed_format

private val IoErrorCodes =
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED until PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED

@Composable
private fun Playing(
    controller: MediaController?,
    player: Player,
    preferences: PlayerPreferences,
    /** True from the first frame drawn, because the caller knew before the stream answered. */
    isKnownLive: Boolean,
    onStyle: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
    /** The search for captions this film shipped without, which the model runs and this draws. */
    onlineSubtitles: OnlineSubtitleState,
    subtitleLanguage: String,
    subtitleQuery: String,
    onSearchSubtitles: () -> Unit,
    onDownloadSubtitle: (OpenSubtitleResult) -> Unit,
    onChooseSubtitleLanguage: (String) -> Unit,
    onSearchSubtitlesFor: (String) -> Unit,
    onBack: () -> Unit,
) {
    // Up over the film rather than in the column beside it: a keyboard is drawn by the television
    // across the foot of the screen, and a box in a panel a third of the width would be typed into
    // from behind it.
    var isNamingSubtitle by remember { mutableStateOf(false) }

    if (isNamingSubtitle) {
        NameSubtitle(
            term = subtitleQuery,
            onSearch = onSearchSubtitlesFor,
            onDismiss = { isNamingSubtitle = false },
        )
    }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var positionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0)) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    // Told, or found out. A channel says so before it starts; anything else that turns out to be a
    // stream -- an .m3u8 sitting on a share -- only announces itself once its timeline arrives, so
    // the reading stays as the fallback rather than being replaced by the flag.
    val readIsLive = { isKnownLive || player.isCurrentMediaItemLive }
    var isLive by remember { mutableStateOf(readIsLive()) }
    var speed by remember { mutableFloatStateOf(player.playbackParameters.speed) }
    // The film's own shape, which the fitting is measured against. Null until the first frame has
    // been decoded, and the modifier leaves the surface alone until then rather than fitting it to
    // a guess.
    var sourceSize by remember { mutableStateOf<Size?>(null) }
    var hasPrevious by remember { mutableStateOf(player.hasPreviousMediaItem()) }
    var hasNext by remember { mutableStateOf(player.hasNextMediaItem()) }
    // Three answers and not two: there is a picture, there is none, and it is too early to say.
    // The third is most of the first few seconds of anything opened over a network, and answering
    // it with "none" is what put a music mark over every film while it was still being reached.
    var hasVideo by remember { mutableStateOf<Boolean?>(null) }
    var title by remember { mutableStateOf(player.mediaMetadata.title?.toString().orEmpty()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                speed = playbackParameters.speed
            }

            // Asked again on a change of state, because the first answer can be missed: the
            // controller is told to play before this screen has composed, and a channel that takes
            // ten seconds to buffer announces itself in that gap. Without this the panel stayed up
            // over a film that was running, with a play mark on a picture that was moving.
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = player.isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                isLive = readIsLive()
                hasPrevious = player.hasPreviousMediaItem()
                hasNext = player.hasNextMediaItem()
            }

            override fun onTracksChanged(tracks: Tracks) {
                // An empty set is the player between two items, not a file with nothing in it.
                // Taken as an answer it says "no picture" for the moment a film takes to open.
                if (tracks.groups.isNotEmpty()) {
                    hasVideo = tracks.groups.any { it.type == C.TRACK_TYPE_VIDEO }
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                title = mediaMetadata.title?.toString().orEmpty()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // Nothing is unlearned by a report of nothing. A film is re-prepared whenever a
                // caption file is put on it -- the item is rebuilt with the new track and swapped
                // for the old one -- and the renderer says "size unknown" in the gap. Read as an
                // answer, that threw the fitting away and the picture stretched to the box for as
                // long as it took the real size to come back.
                if (videoSize.width <= 0 || videoSize.height <= 0) return
                sourceSize = Size(videoSize.width.toFloat(), videoSize.height.toFloat())
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                isLive = readIsLive()
                hasPrevious = player.hasPreviousMediaItem()
                hasNext = player.hasNextMediaItem()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Polled while the bar is up, and never for a channel: the bar shows a badge rather than a
    // clock there, so the reading would be a wake-up twice a second for a number nobody is shown.
    var isBarVisible by remember { mutableStateOf(true) }
    // A stack: the subtitle list is a menu of lists, and back should climb one step rather than
    // throwing away every step the viewer took to get there.
    var selectorStack by remember { mutableStateOf(emptyList<TvSelector>()) }
    val selector = selectorStack.lastOrNull()
    var lastPressMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isBarVisible, isLive, player) {
        while (isBarVisible && !isLive) {
            positionMs = player.currentPosition.coerceAtLeast(0)
            durationMs = player.duration.coerceAtLeast(0)
            delay(TvTickMs)
        }
    }

    // Held open while paused. A stopped picture with nothing on it gives a viewer no way to tell a
    // pause from a film that has died.
    LaunchedEffect(isPlaying, lastPressMs, selector) {
        if (selector != null) return@LaunchedEffect
        if (!isPlaying) {
            isBarVisible = true
            return@LaunchedEffect
        }
        if (!isBarVisible) return@LaunchedEffect
        delay(AutoHideMs)
        isBarVisible = false
    }

    fun poke() {
        isBarVisible = true
        lastPressMs = System.currentTimeMillis()
    }

    /**
     * Back closes a list, or leaves the film. It does not put the bar away.
     *
     * The way a television's own players have it. The bar is not a place a viewer went -- it
     * appeared because they touched the remote and it goes on its own a few seconds later -- so
     * spending a press on it means the one press everybody knows takes two, and taking the film
     * away is not what the second one was for.
     */
    BackHandler {
        if (selectorStack.isNotEmpty()) selectorStack = selectorStack.dropLast(1) else onBack()
    }

    var subtitleDelayMs by remember { mutableLongStateOf(0L) }
    var scale by remember { mutableStateOf(VideoContentScale.BestFit) }
    var isNightMode by remember { mutableStateOf(false) }
    var sleepMinutes by remember { mutableStateOf<Int?>(null) }
    var translateTo by remember { mutableStateOf<String?>(null) }
    var boostMillibels by remember { mutableIntStateOf(NoVolumeBoost) }

    // Asked once, and only offered where the answer is yes: a list offering to make a film louder on
    // a device whose framework will not amplify is a control that lies about what it did.
    var canBoost by remember { mutableStateOf(false) }
    LaunchedEffect(controller) { canBoost = controller?.isVolumeBoostSupported() == true }
    LaunchedEffect(boostMillibels, controller) { controller?.setVolumeBoost(boostMillibels) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickSubtitle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        val file = picked ?: return@rememberLauncherForActivityResult
        scope.launch { controller?.addSubtitle(context.externalSubtitle(file)) }
    }
    // Not every television has a file manager on it, and a row that opens nothing is worse than no
    // row: the viewer presses it, the screen does not move, and they press it again.
    val canPickFile = remember(context) { context.canOpenDocument() }

    // The shift the renderer is asked for, and the reason it is not simply the viewer's number:
    // translating pulls every line forward so the round trip to the translator finishes before the
    // line is due, and the overlay holds the finished text back to the moment it was written for.
    // Said again on a timer because anything that rebuilds the text renderer drops it -- a track
    // chosen, a subtitle added, a seek.
    LaunchedEffect(subtitleDelayMs, translateTo, controller) {
        val session = controller ?: return@LaunchedEffect
        val wanted = subtitleDelayMs - if (translateTo != null) TranslationLookaheadMs else 0L
        while (true) {
            session.setSubtitleDelay(wanted)
            if (translateTo == null) return@LaunchedEffect
            delay(LookaheadRefreshMs)
        }
    }

    LaunchedEffect(isNightMode, controller) { controller?.setNightMode(isNightMode) }
    LaunchedEffect(sleepMinutes, controller) {
        controller?.setSleepTimer(sleepMinutes ?: PlaybackCommands.Off)
    }

    val audioTracks = rememberTracksState(player, C.TRACK_TYPE_AUDIO)
    val textTracks = rememberTracksState(player, C.TRACK_TYPE_TEXT)

    val focus = remember { FocusRequester() }
    // Handed back and forth: the panel's play button asks for focus as it appears, and the screen
    // asks for it back as the panel goes, or the next press would land on a button nobody can see.
    LaunchedEffect(isBarVisible) { if (!isBarVisible) runCatching { focus.requestFocus() } }

    // What opening a list does to the film: it steps aside rather than being covered. The old
    // player splits the screen the same way, and on a television that is the difference between
    // reading a list and squinting past one.
    val isSplit = selector != null
    val filmWeight by animateFloatAsState(
        targetValue = if (isSplit) FilmShare else WholeScreen,
        animationSpec = tween(SplitMs),
        label = "film-weight",
    )
    val panelWeight by animateFloatAsState(
        targetValue = if (isSplit) PanelShare else Hairline,
        animationSpec = tween(SplitMs),
        label = "panel-weight",
    )
    val filmInset by animateDpAsState(
        targetValue = if (isSplit) SplitInset else 0.dp,
        animationSpec = tween(SplitMs),
        label = "film-inset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                // Back is the one key that does not wake the panel. It is handled below, where it
                // puts the panel away and then leaves; waking it here first meant every press put
                // back exactly what the press was about to take down, and the player could not be
                // left at all.
                if (event.key == Key.Back) return@onPreviewKeyEvent false
                // Every other press wakes the panel and then goes on to whatever holds the focus.
                // Only the remote's own media keys are answered here, because nothing on screen
                // owns them.
                poke()
                when (event.key) {
                    Key.MediaPlayPause -> if (player.isPlaying) player.pause() else player.play()
                    Key.MediaNext -> player.stepToNext()
                    Key.MediaPrevious -> player.stepToPrevious()
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(filmWeight)
                    .padding(filmInset)
                    // Kept inside its half. A scale that fills or crops makes the surface larger
                    // than the box it was given, and nothing was cutting it off: the picture grew
                    // out over the panel beside it, so choosing how the film should sit painted
                    // over the list being chosen from.
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                PlayerSurface(
                    player = player,
                    modifier = Modifier
                        .fillMaxSize()
                        .resizeWithContentScale(scale.toContentScale(), sourceSize),
                )
                // A black rectangle with a seek bar under it reads as a film that failed. What
                // goes over it depends on which of the three answers is in: a word while the file
                // is still being reached -- which on a share or a channel is seconds, and was the
                // one moment this screen said nothing at all -- and the music mark only once the
                // tracks have confirmed there is no picture coming.
                when (hasVideo) {
                    null -> TvMessage(stringResource(R.string.opening_media))
                    false -> NowPlaying(title = title, icon = VayouIcons.Audio)
                    true -> Unit
                }
                SubtitleOverlay(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                    translateTo = translateTo,
                    style = preferences,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(panelWeight)
                    .padding(vertical = SplitInset, horizontal = if (isSplit) SplitInset else 0.dp),
            ) {
                // Asked for when the panel opens rather than by the row that opens it: a row that
                // leads somewhere does not answer anything, and the answer is what this one needs.
                LaunchedEffect(selector) {
                    // Again after a failure, because closing the panel and opening it is what
                    // anybody does when something did not work, and finding the same apology
                    // waiting there says the app did not even try.
                    val isUnanswered = onlineSubtitles == OnlineSubtitleState.Idle ||
                        onlineSubtitles == OnlineSubtitleState.Failed
                    if (selector == TvSelector.SubtitleSearch && isUnanswered) onSearchSubtitles()
                }
                selector?.let { open ->
                    TvPlayerSelector(
                        title = stringResource(open.title),
                        message = subtitleSearchMessage(open, onlineSubtitles),
                        options = open.optionsFor(
                            player = player,
                            audio = audioTracks,
                            text = textTracks,
                            delayMs = subtitleDelayMs,
                            translateTo = translateTo,
                            preferences = preferences,
                            boostMillibels = boostMillibels.takeIf { canBoost },
                            scale = scale,
                            isNightMode = isNightMode,
                            sleepMinutes = sleepMinutes,
                            onDelay = { subtitleDelayMs = it },
                            onTranslate = { translateTo = it },
                            onStyle = onStyle,
                            onBoost = { boostMillibels = it },
                            onPickSubtitle = { pickSubtitle.launch(SubtitleMimeTypes) }.takeIf { canPickFile },
                            onlineSubtitles = onlineSubtitles,
                            subtitleLanguage = subtitleLanguage,
                            subtitleQuery = subtitleQuery,
                            onDownloadSubtitle = onDownloadSubtitle,
                            onChooseSubtitleLanguage = onChooseSubtitleLanguage,
                            onEditSubtitleQuery = { isNamingSubtitle = true },
                            onScale = { scale = it },
                            onNightMode = { isNightMode = it },
                            onSleep = { sleepMinutes = it },
                        ),
                        onOpen = { selectorStack = selectorStack + it },
                        // One step, not all of them. Choosing an answer inside a menu should leave
                        // the viewer in the menu they walked into, and back should climb rather
                        // than throw away every step it took to get there.
                        onDismiss = { selectorStack = selectorStack.dropLast(1) },
                    )
                }
            }
        }

        // Gone while a list is open rather than stacked behind it. Two panels of buttons on one
        // screen is two places the focus could be, and only one of them is being read.
        AnimatedVisibility(visible = isBarVisible && !isSplit, enter = fadeIn(), exit = fadeOut()) {
            TvPlayerControls(
                isPlaying = isPlaying,
                isLive = isLive,
                speed = speed,
                positionMs = positionMs,
                durationMs = durationMs,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    poke()
                },
                onPrevious = {
                    player.stepToPrevious()
                    poke()
                },
                onNext = {
                    player.stepToNext()
                    poke()
                },
                onSeek = { target ->
                    player.seekTo(target)
                    poke()
                },
                onOpen = { selectorStack = listOf(it) },
            )
        }
    }
}

/** Whether anything on this device will show the viewer their own files. */
private fun Context.canOpenDocument(): Boolean = Intent(Intent.ACTION_OPEN_DOCUMENT)
    .apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }
    .resolveActivity(packageManager) != null

private fun mediaItem(uri: String, title: String): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setMediaId(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
    .build()

private const val AutoHideMs = 4_000L

/** The heading over each list. */
private val TvSelector.title: Int
    get() = when (this) {
        TvSelector.Audio -> R.string.audio_track
        TvSelector.Subtitle -> R.string.subtitles
        TvSelector.SubtitleTracks -> R.string.subtitle_track
        TvSelector.SubtitleSearch -> R.string.search_subtitle
        TvSelector.SubtitleLanguage -> R.string.subtitle_language
        TvSelector.SubtitleDelay -> R.string.subtitle_delay
        TvSelector.Translation -> R.string.translation
        TvSelector.SubtitleStyle -> R.string.subtitle_style
        TvSelector.SubtitleSize -> R.string.subtitle_size
        TvSelector.Speed -> R.string.speed
        TvSelector.Playlist -> R.string.playlist
        TvSelector.More -> R.string.more
        TvSelector.Scale -> R.string.scale
        TvSelector.Repeat -> R.string.repeat_mode
        TvSelector.SleepTimer -> R.string.sleep_timer
        TvSelector.VolumeBoost -> R.string.volume_boost
    }

/**
 * What each list offers.
 *
 * Audio has no "off": a film playing silently is a fault rather than a choice. Subtitles do, and it
 * comes first, because none is where most films start.
 */
@Composable
private fun TvSelector.optionsFor(
    player: Player,
    audio: TracksState,
    text: TracksState,
    delayMs: Long,
    translateTo: String?,
    preferences: PlayerPreferences,
    /** Null where this device will not amplify, which is what leaves the row off the menu. */
    boostMillibels: Int?,
    scale: VideoContentScale,
    isNightMode: Boolean,
    sleepMinutes: Int?,
    onDelay: (Long) -> Unit,
    onTranslate: (String?) -> Unit,
    onStyle: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
    onBoost: (Int) -> Unit,
    /** Null where nothing on this device can open a file, for the same reason [boostMillibels] is. */
    onPickSubtitle: (() -> Unit)?,
    onlineSubtitles: OnlineSubtitleState,
    subtitleLanguage: String,
    subtitleQuery: String,
    onDownloadSubtitle: (OpenSubtitleResult) -> Unit,
    onChooseSubtitleLanguage: (String) -> Unit,
    onEditSubtitleQuery: () -> Unit,
    onScale: (VideoContentScale) -> Unit,
    onNightMode: (Boolean) -> Unit,
    onSleep: (Int?) -> Unit,
): List<TvSelectorOption> = when (this) {
    TvSelector.Audio -> audio.tracks.map { track ->
        TvSelectorOption(track.label, track.isSelected) { audio.select(track) }
    }

    // A menu rather than the tracks themselves, as the old player has it: what a viewer wants from
    // "subtitles" is as often the timing or the language as the track.
    TvSelector.Subtitle -> buildList {
        add(
            TvSelectorOption(
                label = stringResource(R.string.subtitle_track),
                subLabel = text.tracks.firstOrNull { it.isSelected }?.label ?: stringResource(R.string.track_off),
                icon = VayouIcons.Caption,
                opens = TvSelector.SubtitleTracks,
            ),
        )
        // Beside the tracks the film carries, because to a viewer it is the same question: which
        // words appear. That it came off a memory stick rather than out of the container is the
        // player's business.
        onPickSubtitle?.let { pick ->
            add(TvSelectorOption(stringResource(R.string.subtitle_file), icon = VayouIcons.FileOpen, onChoose = pick))
        }
        // No file manager needed for this one, so it is offered whether or not the last row was.
        // Most of what a television plays comes off a share and arrives with no captions at all.
        add(
            TvSelectorOption(
                label = stringResource(R.string.search_subtitle),
                icon = VayouIcons.Search,
                opens = TvSelector.SubtitleSearch,
            ),
        )
        add(
            TvSelectorOption(
                label = stringResource(R.string.subtitle_delay),
                subLabel = delayLabel(delayMs),
                icon = VayouIcons.Timer,
                opens = TvSelector.SubtitleDelay,
            ),
        )
        add(
            TvSelectorOption(
                label = stringResource(R.string.translation),
                subLabel = translateTo?.let(::languageName) ?: stringResource(R.string.track_off),
                icon = VayouIcons.Language,
                opens = TvSelector.Translation,
            ),
        )
        // The ready-made styles and nothing finer. Colour, outline, background and position are
        // each a list walked with a D-pad from three metres, and nobody adjusts an outline that
        // way: they pick one they can read and go back to the film. The fine controls stay on the
        // phone, where a thumb makes them worth having.
        add(
            TvSelectorOption(
                label = stringResource(R.string.subtitle_style),
                subLabel = stringResource(SubtitlePreset.entries.firstOrNull { it.matches(preferences) }.tvLabel),
                icon = VayouIcons.Style,
                opens = TvSelector.SubtitleStyle,
            ),
        )
        add(
            TvSelectorOption(
                label = stringResource(R.string.subtitle_size),
                subLabel = stringResource(sizePreset(preferences.subtitleTextSize).tvLabel),
                icon = VayouIcons.Size,
                opens = TvSelector.SubtitleSize,
            ),
        )
    }

    TvSelector.SubtitleStyle -> SubtitlePreset.entries.map { preset ->
        TvSelectorOption(
            label = stringResource(preset.tvLabel),
            isSelected = preset.matches(preferences),
            sample = preset,
        ) { onStyle { preset.applyTo(it) } }
    }

    TvSelector.SubtitleSize -> SubtitleSizePreset.entries.map { size ->
        TvSelectorOption(stringResource(size.tvLabel), size.textSize == preferences.subtitleTextSize) {
            onStyle { it.copy(subtitleTextSize = size.textSize) }
        }
    }

    // The results, with the language filter at the head of them: a viewer who got English for a
    // Brazilian film wants to narrow it, and that is the next thing they reach for.
    TvSelector.SubtitleSearch -> buildList {
        add(
            TvSelectorOption(
                label = stringResource(R.string.subtitle_search_term),
                subLabel = subtitleQuery,
                icon = VayouIcons.Search,
                onChoose = onEditSubtitleQuery,
            ),
        )
        add(
            TvSelectorOption(
                label = stringResource(R.string.subtitle_language),
                subLabel = SubtitleLanguages.firstOrNull { it.id == subtitleLanguage }?.label,
                icon = VayouIcons.Language,
                opens = TvSelector.SubtitleLanguage,
            ),
        )
        val found = onlineSubtitles as? OnlineSubtitleState.Found ?: onlineSubtitles as? OnlineSubtitleState.Downloading
        val results = when (found) {
            is OnlineSubtitleState.Found -> found.results
            is OnlineSubtitleState.Downloading -> found.results
            else -> emptyList()
        }
        results.forEach { result ->
            add(
                TvSelectorOption(
                    label = result.subFileName,
                    // What release it matched and how many people took it, which between them are
                    // the whole of what tells one line of this list from the next.
                    subLabel = "${result.subLanguageId.uppercase()} · ${result.subDownloadsCnt}",
                    icon = VayouIcons.Caption,
                ) { onDownloadSubtitle(result) },
            )
        }
    }

    TvSelector.SubtitleLanguage -> SubtitleLanguages.map { language ->
        TvSelectorOption(language.label, isSelected = language.id == subtitleLanguage) {
            onChooseSubtitleLanguage(language.id)
        }
    }

    TvSelector.SubtitleTracks -> buildList {
        add(TvSelectorOption(stringResource(R.string.track_off), text.isOff, onChoose = text::turnOff))
        text.tracks.forEach { track ->
            add(TvSelectorOption(track.label, track.isSelected) { text.select(track) })
        }
    }

    TvSelector.SubtitleDelay -> SubtitleDelays.map { millis ->
        TvSelectorOption(delayLabel(millis), millis == delayMs) { onDelay(millis) }
    }

    TvSelector.Translation -> buildList {
        add(TvSelectorOption(stringResource(R.string.track_off), translateTo == null) { onTranslate(null) })
        TranslationLanguages.forEach { code ->
            add(TvSelectorOption(languageName(code), code == translateTo) { onTranslate(code) })
        }
    }

    TvSelector.Speed -> PlaybackSpeeds.map { speed ->
        TvSelectorOption(
            label = speed.asSpeedLabel(),
            isSelected = kotlin.math.abs(player.playbackParameters.speed - speed) < SpeedSlack,
            onChoose = { player.setPlaybackSpeed(speed) },
        )
    }

    // The drawer for everything that is neither a track nor a speed. A television's remote has no
    // room for a button each, and none of these is reached often enough to earn one.
    TvSelector.More -> buildList {
        add(
            TvSelectorOption(
                label = stringResource(R.string.scale),
                subLabel = stringResource(scale.tvLabel),
                icon = VayouIcons.Size,
                opens = TvSelector.Scale,
            ),
        )
        boostMillibels?.let { boost ->
            add(
                TvSelectorOption(
                    label = stringResource(R.string.volume_boost),
                    subLabel = boostLabel(boost),
                    icon = VayouIcons.VolumeUp,
                    opens = TvSelector.VolumeBoost,
                ),
            )
        }
        add(
            TvSelectorOption(
                label = stringResource(R.string.repeat_mode),
                subLabel = stringResource(player.repeatMode.repeatLabel),
                icon = VayouIcons.Repeat,
                opens = TvSelector.Repeat,
            ),
        )
        add(
            TvSelectorOption(
                label = stringResource(R.string.sleep_timer),
                subLabel = sleepMinutes?.let { stringResource(R.string.minutes, it) }
                    ?: stringResource(R.string.track_off),
                icon = VayouIcons.Timer,
                opens = TvSelector.SleepTimer,
            ),
        )
        // A switch rather than a list: it is on or it is off, and walking into a list of two to say
        // so is a press wasted.
        add(
            TvSelectorOption(
                label = stringResource(R.string.night_mode),
                subLabel = stringResource(if (isNightMode) R.string.on else R.string.track_off),
                icon = VayouIcons.DarkMode,
                onChoose = { onNightMode(!isNightMode) },
            ),
        )
    }

    TvSelector.Scale -> VideoContentScale.entries.map { option ->
        TvSelectorOption(stringResource(option.tvLabel), option == scale) { onScale(option) }
    }

    TvSelector.Repeat -> RepeatModes.map { mode ->
        TvSelectorOption(stringResource(mode.repeatLabel), mode == player.repeatMode) {
            player.repeatMode = mode
        }
    }

    TvSelector.SleepTimer -> buildList {
        add(TvSelectorOption(stringResource(R.string.track_off), sleepMinutes == null) { onSleep(null) })
        SleepMinutes.forEach { minutes ->
            add(
                TvSelectorOption(stringResource(R.string.minutes, minutes), minutes == sleepMinutes) {
                    onSleep(minutes)
                },
            )
        }
    }

    TvSelector.VolumeBoost -> VolumeBoosts.map { millibels ->
        TvSelectorOption(boostLabel(millibels), millibels == boostMillibels) { onBoost(millibels) }
    }

    TvSelector.Playlist -> (0 until player.mediaItemCount).map { index ->
        TvSelectorOption(
            label = player.getMediaItemAt(index).mediaMetadata.title?.toString().orEmpty(),
            isSelected = index == player.currentMediaItemIndex,
            onChoose = { player.seekTo(index, C.TIME_UNSET) },
        )
    }
}

/** What each ready-made caption style is called here. Null is a style the viewer built by hand. */
private val SubtitlePreset?.tvLabel: Int
    get() = when (this) {
        SubtitlePreset.Raised -> R.string.subtitle_preset_raised
        SubtitlePreset.Outlined -> R.string.subtitle_preset_outlined
        SubtitlePreset.DropShadow -> R.string.subtitle_preset_shadow
        SubtitlePreset.Contrast -> R.string.subtitle_preset_contrast
        SubtitlePreset.Light -> R.string.subtitle_preset_light
        SubtitlePreset.Box -> R.string.subtitle_preset_box
        null -> R.string.subtitle_preset_custom
    }

private val SubtitleSizePreset.tvLabel: Int
    get() = when (this) {
        SubtitleSizePreset.Small -> R.string.subtitle_size_small
        SubtitleSizePreset.Medium -> R.string.subtitle_size_medium
        SubtitleSizePreset.Large -> R.string.subtitle_size_large
    }

/** The rung a size sits on, or the nearest below it when the phone set something in between. */
private fun sizePreset(textSize: Int): SubtitleSizePreset =
    SubtitleSizePreset.entries.lastOrNull { it.textSize <= textSize } ?: SubtitleSizePreset.Small

/** What each way of fitting a film is called on a television. */
private val VideoContentScale.tvLabel: Int
    get() = when (this) {
        VideoContentScale.BestFit -> R.string.scale_best_fit
        VideoContentScale.Stretch -> R.string.scale_stretch
        VideoContentScale.Crop -> R.string.scale_crop
        VideoContentScale.HundredPercent -> R.string.scale_hundred_percent
    }

private val Int.repeatLabel: Int
    get() = when (this) {
        Player.REPEAT_MODE_ONE -> R.string.repeat_one
        Player.REPEAT_MODE_ALL -> R.string.repeat_all
        else -> R.string.track_off
    }

/** Signed, because the sign is the whole of what a viewer is choosing between. */
private fun delayLabel(millis: Long): String = when {
    millis == 0L -> "0 s"
    millis > 0L -> "+%.1f s".format(millis / 1000f)
    else -> "%.1f s".format(millis / 1000f)
}

/** As a share of the device's own maximum, which is how the phone's readout counts it too. */
@Composable
private fun boostLabel(millibels: Int): String = if (millibels == NoVolumeBoost) {
    stringResource(R.string.track_off)
} else {
    "+${millibels * FullVolumePercent / MaxVolumeBoostMillibels}%"
}

private fun languageName(code: String): String = Locale.forLanguageTag(code).displayLanguage

/** The same rungs the phone offers, so a film watched on both behaves the same on either. */
private val PlaybackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

/** Half a second a step, which is the size of the mistake a subtitle file usually has in it. */
private val SubtitleDelays = (-10..10).map { it * 500L }

/** What the phone's own list offers, and no more: a wall of languages is not a choice. */
private val TranslationLanguages = listOf("en", "pt", "es", "fr", "de", "it", "ja", "ko", "zh", "ru")

private const val SpeedSlack = 0.001f

private val RepeatModes = listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_ALL)

/** Quarters of what the framework will give, which is as fine as anyone amplifying a quiet film
 *  needs: below a quarter there is nothing to hear, and the steps between are guesswork either way. */
private val VolumeBoosts = listOf(NoVolumeBoost, 500, 1_000, 1_500, MaxVolumeBoostMillibels)

/** The device's own maximum, as the readout counts it: the whole boost on top of it is another 100. */
private const val FullVolumePercent = 100

/** Around a film's length, and around a night: what a viewer falling asleep would pick. */
private val SleepMinutes = listOf(15, 30, 45, 60, 90, 120)

/** Six times a minute, which is nothing beside being wrong for the rest of the film. */
private const val LookaheadRefreshMs = 10_000L

/** What the film keeps of the width while a list is beside it. */
private const val FilmShare = 0.58f

private const val PanelShare = 0.42f

private val SplitInset = 24.dp

/**
 * The name to look the film up by, when the one on the file is not one anybody would recognise.
 *
 * Starts from whatever was searched last rather than empty: most of the time it wants a word
 * added or a release tag taken off, not typing from nothing on a D-pad.
 */
@Composable
private fun NameSubtitle(term: String, onSearch: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable(term) { mutableStateOf(term) }
    val field = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }

    TvDialog(title = stringResource(R.string.subtitle_search_term), onDismiss = onDismiss) {
        TvTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.subtitle_search_term),
            modifier = Modifier.focusRequester(field),
        )
        Spacer(modifier = Modifier.height(TvRowGap))
        TvActions {
            TvAction(stringResource(R.string.cancel), onClick = onDismiss)
            TvAction(stringResource(R.string.search)) {
                onSearch(text)
                onDismiss()
            }
        }
    }
}

/**
 * What a search panel says while it has no list to show.
 *
 * Null once there are results, which is when the list itself is the answer.
 */
@Composable
private fun subtitleSearchMessage(open: TvSelector, state: OnlineSubtitleState): String? {
    if (open != TvSelector.SubtitleSearch) return null
    return when (state) {
        OnlineSubtitleState.Idle, OnlineSubtitleState.Searching -> stringResource(R.string.searching)
        OnlineSubtitleState.Failed -> stringResource(R.string.subtitle_search_failed)
        is OnlineSubtitleState.Found ->
            stringResource(R.string.no_subtitles_found).takeIf { state.results.isEmpty() }
        is OnlineSubtitleState.Downloading -> null
    }
}
