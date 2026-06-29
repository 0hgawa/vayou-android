package dev.vayou.feature.player.cast

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import java.io.File
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class CastState { Idle, Connecting, Connected, Disconnecting }

enum class CastError { ConnectFailed, ResumeLocalFailed, PlayerInitFailed }

@UnstableApi
object CastSessionManager {

    var castPlayer by mutableStateOf<CastPlayer?>(null)
        private set

    var deviceName by mutableStateOf<String?>(null)
        private set

    var state by mutableStateOf(CastState.Idle)
        private set

    private val _errors = MutableSharedFlow<CastError>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errors: SharedFlow<CastError> = _errors.asSharedFlow()

    private var castContext: CastContext? = null
    private var castMediaServer: CastMediaServer? = null
    private var contentResolver: ContentResolver? = null
    private var cacheDir: File? = null
    private var appContext: Context? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    val isAvailable: Boolean get() = castContext != null
    val isConnected: Boolean get() = state == CastState.Connected
    val isConnecting: Boolean get() = state == CastState.Connecting

    fun initialize(context: Context) {
        if (castContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        contentResolver = ctx.contentResolver
        cacheDir = ctx.cacheDir
        wifiLock = (ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Vayou:CastServer")
        wakeLock = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Vayou:CastServer")
        castContext = try {
            CastContext.getSharedInstance(ctx).also { castCtx ->
                castCtx.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
                castCtx.sessionManager.currentCastSession?.let { onSessionActive(it) }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun addActivitySessionListener(listener: SessionManagerListener<CastSession>) {
        castContext?.sessionManager?.addSessionManagerListener(listener, CastSession::class.java)
    }

    fun removeActivitySessionListener(listener: SessionManagerListener<CastSession>) {
        castContext?.sessionManager?.removeSessionManagerListener(listener, CastSession::class.java)
    }

    fun getOrCreatePlayer(): CastPlayer? {
        castPlayer?.let { return it }
        val ctx = castContext ?: return null
        if (ctx.sessionManager.currentCastSession == null) return null
        val cr = contentResolver ?: return null

        val server = castMediaServer ?: CastMediaServer(cr, cacheDir).also {
            it.ensureStarted()
            castMediaServer = it
        }

        return CastPlayer(ctx, createConverter(server)).also { castPlayer = it }
    }

    fun transferFromLocal(localPlayer: Player) {
        val cp = getOrCreatePlayer() ?: run {
            _errors.tryEmit(CastError.PlayerInitFailed)
            return
        }
        val itemCount = localPlayer.mediaItemCount
        if (itemCount == 0) return
        val items = ArrayList<MediaItem>(itemCount).apply {
            for (i in 0 until itemCount) add(localPlayer.getMediaItemAt(i))
        }

        val position = localPlayer.currentPosition
        val currentIndex = localPlayer.currentMediaItemIndex
        val activeFormatId = findSelectedTextFormatId(localPlayer)

        // stop() puts the local player in STATE_IDLE so its listeners stop firing
        // STATE_ENDED / PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM mid-cast
        // (which otherwise kill the activity). Items stay queued so transferToLocal
        // can just seekTo+prepare when the cast session ends.
        localPlayer.stop()

        cp.setMediaItems(items, currentIndex, position)
        cp.playWhenReady = true
        cp.prepare()

        if (activeFormatId != null) {
            val trackId = resolveSubtitleTrackId(items[currentIndex], activeFormatId)
            if (trackId != null) applyActiveSubtitle(cp, trackId)
        }
    }

    fun transferToLocal(localPlayer: Player): Boolean {
        val cp = castPlayer ?: return false
        val position = cp.currentPosition
        val currentIndex = cp.currentMediaItemIndex

        cp.release()
        castPlayer = null

        if (localPlayer.mediaItemCount == 0) {
            _errors.tryEmit(CastError.ResumeLocalFailed)
            return false
        }
        localPlayer.seekTo(currentIndex, position)
        localPlayer.playWhenReady = true
        localPlayer.prepare()
        return true
    }

    fun stopPlayback() {
        castPlayer?.release()
        castPlayer = null
        castMediaServer?.stop()
        castMediaServer = null
        releaseLocks()
        appContext?.let { CastForegroundService.stop(it) }
        runCatching { castContext?.sessionManager?.endCurrentSession(true) }
    }

    private fun onSessionActive(session: CastSession) {
        if (state == CastState.Connected) return
        deviceName = session.castDevice?.friendlyName
        state = CastState.Connected
        wifiLock?.takeIf { !it.isHeld }?.runCatching { acquire() }
        wakeLock?.takeIf { !it.isHeld }?.runCatching { acquire() }
        appContext?.let { CastForegroundService.start(it) }
    }

    private fun onSessionEnded() {
        if (state == CastState.Idle) return
        deviceName = null
        state = CastState.Idle
        releaseLocks()
        appContext?.let { CastForegroundService.stop(it) }
        castPlayer?.release()
        castPlayer = null
        castMediaServer?.stop()
        castMediaServer = null
    }

    private fun releaseLocks() {
        wifiLock?.takeIf { it.isHeld }?.runCatching { release() }
        wakeLock?.takeIf { it.isHeld }?.runCatching { release() }
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) { state = CastState.Connecting }
        override fun onSessionStarted(session: CastSession, sessionId: String) = onSessionActive(session)
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _errors.tryEmit(CastError.ConnectFailed)
            onSessionEnded()
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) { state = CastState.Connecting }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = onSessionActive(session)
        override fun onSessionResumeFailed(session: CastSession, error: Int) = onSessionEnded()
        override fun onSessionSuspended(session: CastSession, reason: Int) { state = CastState.Connecting }
        override fun onSessionEnding(session: CastSession) { state = CastState.Disconnecting }
        override fun onSessionEnded(session: CastSession, error: Int) = onSessionEnded()
    }

    private fun findSelectedTextFormatId(player: Player): String? {
        for (group in player.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) return group.getTrackFormat(i).id
            }
        }
        return null
    }

    private fun resolveSubtitleTrackId(item: MediaItem, formatId: String): Long? {
        val subs = item.localConfiguration?.subtitleConfigurations ?: return null
        for (i in subs.indices) {
            val sub = subs[i]
            val subId = sub.id
            val uriStr = sub.uri.toString()
            if (formatId == subId ||
                formatId == uriStr ||
                (subId != null && formatId.endsWith(":$subId")) ||
                formatId.endsWith(":$uriStr")
            ) {
                return (i + 1).toLong()
            }
        }
        return null
    }

    private fun applyActiveSubtitle(cp: CastPlayer, trackId: Long) {
        // Media3 CastPlayer doesn't expose text-track activation. Delegate to the
        // RemoteMediaClient — if the media hasn't loaded on the receiver yet,
        // wait for STATE_READY once and then apply.
        val client = castContext?.sessionManager?.currentCastSession?.remoteMediaClient ?: return
        if (client.mediaStatus != null) {
            runCatching { client.setActiveMediaTracks(longArrayOf(trackId)) }
            return
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_READY) return
                cp.removeListener(this)
                runCatching { client.setActiveMediaTracks(longArrayOf(trackId)) }
            }
        }
        cp.addListener(listener)
    }

    private fun createConverter(server: CastMediaServer) = object : MediaItemConverter {
        override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
            val videoUri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY
            val isRemote = videoUri.scheme in listOf("http", "https", "rtsp", "rtmp")
            val videoFilename = videoUri.lastPathSegment
            val streamUrl = if (isRemote) videoUri.toString()
                else server.getStreamUrl(videoUri, videoFilename)
            val mimeType = mediaItem.localConfiguration?.mimeType ?: CastMediaServer.mimeTypeForUri(videoUri)

            val title = mediaItem.mediaMetadata.title?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: videoFilename
                ?: ""
            val metadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(CastMediaMetadata.KEY_TITLE, title)
            }

            val builder = MediaInfo.Builder(streamUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mimeType)
                .setMetadata(metadata)

            val subs = mediaItem.localConfiguration?.subtitleConfigurations
            if (!subs.isNullOrEmpty()) {
                // Chromecast's CAF receiver often refuses subtitles whose URL has no
                // extension, and works best when the subtitle filename shares the
                // video's basename (sidecar convention). Synthesize that for every
                // track regardless of the .srt's real filename on disk.
                val videoBase = videoFilename?.substringBeforeLast('.') ?: "subtitle"
                val tracks = ArrayList<MediaTrack>(subs.size)
                for (i in subs.indices) {
                    val sub = subs[i]
                    val subUri = sub.uri
                    val subMime = sub.mimeType ?: CastMediaServer.mimeTypeForUri(subUri)
                    val ext = subtitleExtensionForMime(subMime)
                    val isRemoteSub = subUri.scheme in listOf("http", "https")
                    val synthesizedName = "$videoBase${if (subs.size > 1) ".${i + 1}" else ""}$ext"
                    val subUrl = if (isRemoteSub) subUri.toString()
                        else server.getStreamUrl(subUri, synthesizedName)
                    tracks += MediaTrack.Builder((i + 1).toLong(), MediaTrack.TYPE_TEXT)
                        .setContentId(subUrl)
                        .setContentType(subMime)
                        .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                        .setName(sub.label?.toString().orEmpty())
                        .setLanguage(sub.language.orEmpty())
                        .build()
                }
                builder.setMediaTracks(tracks)
            }

            return MediaQueueItem.Builder(builder.build()).build()
        }

        private fun subtitleExtensionForMime(mime: String): String = when (mime) {
            "text/vtt" -> ".vtt"
            "application/x-subrip", "text/x-subrip" -> ".srt"
            "text/x-ssa" -> ".ssa"
            "application/ttml+xml" -> ".ttml"
            else -> ".srt"
        }

        override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
            val info = mediaQueueItem.media ?: return MediaItem.EMPTY
            return MediaItem.Builder().setUri(info.contentId).setMediaId(info.contentId).build()
        }
    }
}
