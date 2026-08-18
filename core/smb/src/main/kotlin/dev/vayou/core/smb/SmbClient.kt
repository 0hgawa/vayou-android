package dev.vayou.core.smb

import android.net.Uri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.msfscc.fileinformation.FileStandardInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import java.io.IOException
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** An open handle and the length behind it, which the player needs before it reads a byte. */
class SmbStreamingFile(val file: com.hierynomus.smbj.share.File, val size: Long)

/** The address to play, and the subtitles sitting beside it. */
class StreamingUris(val media: Uri, val subtitles: List<Uri>)

/**
 * Two connections to the machines on this network: one for looking, one for playing.
 *
 * Two and not one, which is the whole shape of this class. Browsing and playback want opposite
 * things from a connection -- the browser hops between shares and machines and is happy to be torn
 * down, while the player holds one file open for two hours and must not be. Sharing a session made
 * every browse a hazard to whatever was playing: walking into another share closed the share the
 * film was being read from, and leaving the network screen closed the session under it.
 *
 * Half the API is `suspend` and half is blocking, and the split is not an accident either: the
 * browser calls the first from a coroutine, and ExoPlayer calls the second from its own loading
 * thread, which has no coroutine to suspend in.
 */
@Singleton
class SmbClient @Inject constructor(private val serverStore: SmbServerStore) {

    private val client = SMBClient(
        SmbConfig.builder()
            .withTimeout(ConnectionTimeoutSec, TimeUnit.SECONDS)
            // Waited on for ever, which is not the oversight it looks like.
            //
            // This is the socket's own read timeout, and the thread it applies to is the one that
            // sits waiting for the server to say something -- so it is not measuring a slow server,
            // it is measuring silence. A connection nobody is using is silent, and a viewer reading
            // the names in a folder before choosing one is doing nothing else. Set to twelve
            // seconds, as it was, the wait expired every time somebody paused; smbj treats that
            // exception as the connection having died and tears it down, so browsing after a pause
            // met a session that had been thrown away underneath it.
            //
            // A server that has actually stopped answering is caught by the timeouts below, which
            // are per request and know what they are waiting for.
            .withSoTimeout(NoSocketTimeout, TimeUnit.SECONDS)
            .withReadTimeout(FileTimeoutSec, TimeUnit.SECONDS)
            .withWriteTimeout(FileTimeoutSec, TimeUnit.SECONDS)
            .withReadBufferSize(ReadBufferSize)
            .build(),
    )

    /** What the viewer is looking at. Freely closed and rebuilt; nothing is reading from it. */
    private val browsing = SmbLink(client)

    /** What a film is being read from. Nothing on the browsing side may touch it. */
    private val streaming = SmbLink(client)

    suspend fun connect(host: String, username: String, password: String, domain: String = ""): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { browsing.connect(host, SmbCredentials(username, password, domain)) }
        }

    suspend fun connectAsGuest(host: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { browsing.connect(host, credentials = null) }
    }

    /**
     * What the server offers.
     *
     * Over DCERPC and not SMB2: the protocol has no call that asks a server to name its shares, so
     * this goes through the same remote-administration service Windows Explorer uses. The hidden
     * ones are dropped -- they are the machine's own, not the viewer's.
     */
    suspend fun listShares(): Result<List<SmbShare>> = withContext(Dispatchers.IO) {
        runCatching {
            retrying {
                browsing.rebuildingOnFailure {
                    val transport = SMBTransportFactories.SRVSVC.getTransport(browsing.session())
                    ServerService(transport).shares0
                        .map { it.netName.trimEnd('/') }
                        .filter { it.isNotBlank() && !it.endsWith(HiddenShareSuffix) }
                        .map(::SmbShare)
                }
            }
        }
    }

    suspend fun listDirectory(shareName: String, path: String = ""): Result<List<SmbFileItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                retrying {
                    browsing.rebuildingOnFailure { browsing.share(shareName).listVisible(path.trimStart('\\', '/')) }
                }
            }
        }

    /**
     * Where to play from, subtitles included.
     *
     * They are found here rather than by the player because finding them means another directory
     * listing on a connection only this class holds.
     */
    suspend fun streamingUris(shareName: String, path: String, fileName: String): Result<StreamingUris> =
        withContext(Dispatchers.IO) {
            runCatching {
                retrying {
                    browsing.rebuildingOnFailure {
                        val host = browsing.host ?: error("Not connected")
                        val directory = path.substringBeforeLast('\\', "")
                        val videoBaseName = fileName.substringBeforeLast('.')
                        StreamingUris(
                            media = smbUri(host, shareName, path),
                            subtitles = browsing.share(shareName).listVisible(directory)
                                .filter { it.isSubtitleFor(videoBaseName) }
                                .map { smbUri(host, shareName, it.path) },
                        )
                    }
                }
            }
        }

    /**
     * Opens a file for the player, on the player's own connection.
     *
     * Credentials are read with [runBlocking] because there is no coroutine here to suspend in --
     * this runs on ExoPlayer's loading thread. It blocks that thread and nothing else: the browsing
     * link has its own lock, so a viewer walking through folders is never waiting on this.
     */
    fun openForStreaming(host: String, shareName: String, filePath: String): SmbStreamingFile {
        val credentials = runBlocking { serverStore.credentials(host) }?.takeIf { it.username.isNotBlank() }
        streaming.reachHost(host, credentials)
        return streaming.rebuildingOnFailure {
            val file = streaming.share(shareName).openReadOnly(filePath)
            SmbStreamingFile(file, file.length)
        }
    }

    /** Leaves the machine the viewer was looking at. What is playing carries on. */
    fun disconnect() = browsing.close()

    private suspend fun <T> retrying(block: suspend () -> T): T {
        var wait = RetryInitialDelayMs
        repeat(MaxAttempts - 1) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (!e.isWorthRetrying) throw e
                delay(wait)
                wait = minOf(wait * 2, RetryMaxDelayMs)
            }
        }
        return block()
    }

    private companion object {
        const val ConnectionTimeoutSec = 8L
        const val NoSocketTimeout = 0L
        const val FileTimeoutSec = 30L

        /** One megabyte per SMB2 READ. Fewer and larger is what lets streaming keep up. */
        const val ReadBufferSize = 1024 * 1024

        const val MaxAttempts = 4
        const val RetryInitialDelayMs = 500L
        const val RetryMaxDelayMs = 8_000L

        const val HiddenShareSuffix = "$"
    }
}

/**
 * One authenticated connection to one machine, and the share last opened on it.
 *
 * Every method is synchronised on this object rather than on the client that owns two of them, so
 * the two never wait on each other.
 */
private class SmbLink(private val client: SMBClient) {

    var host: String? = null
        private set

    private var credentials: SmbCredentials? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var shareName: String? = null

    @Synchronized
    fun connect(host: String, credentials: SmbCredentials?) {
        close()
        open(host, credentials)
    }

    /** Points this link at [host], unless it is already there and still answering. */
    @Synchronized
    fun reachHost(host: String, credentials: SmbCredentials?) {
        if (this.host == host && connection?.isConnected == true) return
        close()
        open(host, credentials)
    }

    @Synchronized
    fun session(): Session = session ?: error("Not connected")

    @Synchronized
    fun share(name: String): DiskShare {
        val existing = share
        if (existing != null && shareName == name && existing.isConnected) return existing
        runCatching { existing?.close() }
        share = null
        shareName = null
        val opened = session().connectShare(name) as DiskShare
        share = opened
        shareName = name
        return opened
    }

    /**
     * Runs [block]; if it throws, throws the connection away and runs it once more.
     *
     * The second attempt is on a socket that is definitely new, which is the point. A server drops
     * an idle session without saying so and goes on reporting the socket as connected, so retrying
     * against the same objects fails the same way for ever -- which is what left the browser stuck
     * on an error until the viewer backed out of the machine entirely.
     */
    @Synchronized
    fun <T> rebuildingOnFailure(block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        val host = host ?: throw e
        val credentials = credentials
        close()
        open(host, credentials)
        block()
    }

    @Synchronized
    fun close() {
        runCatching { share?.close() }
        runCatching { session?.close() }
        runCatching { connection?.close() }
        share = null
        shareName = null
        session = null
        connection = null
    }

    private fun open(host: String, credentials: SmbCredentials?) {
        val opened = client.connect(host)
        connection = opened
        this.host = host
        this.credentials = credentials
        session = opened.authenticate(
            credentials
                ?.let { AuthenticationContext(it.username, it.password.toCharArray(), it.domain) }
                ?: AuthenticationContext.guest(),
        )
    }
}

/**
 * Worth another go.
 *
 * A wrong password will be wrong the second time too, and so will a share that does not exist.
 * Everything else -- a dropped socket, a busy server, a Wi-Fi handover -- is worth waiting out.
 */
private val Throwable.isWorthRetrying: Boolean
    get() {
        val reason = message?.lowercase().orEmpty()
        return Refusals.none { it in reason }
    }

private val Refusals = listOf(
    "access_denied",
    "logon_failure",
    "bad_network_name",
    "bad_network_path",
    "not connected",
)

/**
 * Where a file is, as one address.
 *
 * Built a segment at a time rather than spelled out and parsed, because a file on a share is named
 * by whoever saved it and an address has characters of its own. `18. Piano Sonata N#32.mp3` is a
 * perfectly ordinary track and a `#` opens the fragment of a URL: parsed from a string, everything
 * from the hash onwards was thrown away and the player was handed a file that does not exist. A `?`
 * or a `%` in a name does the same in its own way. Appending encodes each segment, and taking the
 * address apart again with [smbSharePath] decodes them.
 *
 * Public because a share's address format belongs to this module and to nothing else: a caller
 * building the queue of tracks beside the one it was handed needs the same form, and a second place
 * that knew how to spell `smb://` would be a second place to get it wrong.
 */
fun smbUri(host: String, shareName: String, path: String): Uri = Uri.Builder()
    .scheme(SmbScheme)
    .authority(host)
    .appendPath(shareName)
    .apply { path.split('\\', '/').filter { it.isNotEmpty() }.forEach(::appendPath) }
    .build()

/** What marks an address as being on a share rather than on this machine or over the web. */
const val SmbScheme = "smb"

/** The same address taken apart again: the share it names, and the Windows-separated path within. */
fun Uri.smbSharePath(): Pair<String, String> {
    val segments = pathSegments
    val shareName = segments.firstOrNull() ?: throw IOException("Not an SMB address: $this")
    return shareName to segments.drop(1).joinToString("\\")
}

/**
 * What is in [path], without what the machine keeps to itself, in whatever order the share gave.
 *
 * Unordered on purpose. The order belongs to whoever is showing it, and the browser sorts by what
 * the viewer picked -- an order imposed here would be thrown away a screen later, and the one
 * caller that is not the browser is looking for a subtitle beside a film and sorts a whole
 * directory to keep two names.
 */
private fun DiskShare.listVisible(path: String): List<SmbFileItem> = list(path)
    .filter { it.isWorthShowing }
    .map { it.toSmbFileItem(path) }

/**
 * Something a viewer came here to find, rather than something the file system keeps for itself.
 *
 * Asked of the attributes and not of the name, which is what puts `$RECYCLE.BIN` and
 * `System Volume Information` at the top of every Windows share: both are named like anything else
 * and both are marked hidden and system, and a viewer opening one is opening a folder they cannot
 * read. The dot-prefix stays as well, for the shares that are not Windows.
 */
private val FileIdBothDirectoryInformation.isWorthShowing: Boolean
    get() = fileName != "." &&
        fileName != ".." &&
        !fileName.startsWith(".") &&
        fileAttributes and ConcealedAttributes == 0L

private val ConcealedAttributes =
    FileAttributes.FILE_ATTRIBUTE_HIDDEN.value or FileAttributes.FILE_ATTRIBUTE_SYSTEM.value

private fun DiskShare.openReadOnly(path: String) = openFile(
    path,
    EnumSet.of(AccessMask.GENERIC_READ),
    EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
    SMB2CreateDisposition.FILE_OPEN,
    EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
)

private val com.hierynomus.smbj.share.File.length: Long
    get() = getFileInformation(FileStandardInformation::class.java).endOfFile

private fun FileIdBothDirectoryInformation.toSmbFileItem(parentPath: String): SmbFileItem {
    val isDirectory = fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
    return SmbFileItem(
        name = fileName,
        path = if (parentPath.isEmpty()) fileName else "$parentPath\\$fileName",
        isDirectory = isDirectory,
        size = if (isDirectory) 0 else endOfFile,
    )
}
