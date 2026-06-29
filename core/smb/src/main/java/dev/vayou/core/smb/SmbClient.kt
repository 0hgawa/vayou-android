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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SmbStreamingFile(
    val file: com.hierynomus.smbj.share.File,
    val size: Long,
)

@Singleton
class SmbClient @Inject constructor(
    private val serverStore: SmbServerStore,
) {

    private val client = SMBClient(
        SmbConfig.builder()
            .withTimeout(CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS)
            .withSoTimeout(SO_TIMEOUT_SEC, TimeUnit.SECONDS)
            .withReadTimeout(FILE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .withWriteTimeout(FILE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .withReadBufferSize(READ_BUFFER_SIZE)
            .build(),
    )

    private var connection: Connection? = null
    private var session: Session? = null
    private var connectedHost: String? = null
    private var connectedCredentials: SmbCredentials? = null
    private var cachedShare: DiskShare? = null
    private var cachedShareName: String? = null

    suspend fun connect(host: String, username: String, password: String, domain: String = ""): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                disconnect()
                connectInternal(host, SmbCredentials(username, password, domain))
            }
        }

    suspend fun connectAsGuest(host: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            disconnect()
            connectInternal(host, null)
        }
    }

    suspend fun listShares(): Result<List<SmbShare>> = withContext(Dispatchers.IO) {
        runCatching {
            retryWithBackoff {
                ensureConnected()
                val transport = SMBTransportFactories.SRVSVC.getTransport(requireSession())
                ServerService(transport).shares0
                    .map { it.netName.trimEnd('/') }
                    .filter { it.isNotBlank() && !it.endsWith("$") }
                    .map { SmbShare(name = it) }
            }
        }
    }

    suspend fun listDirectory(shareName: String, path: String = ""): Result<List<SmbFileItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                retryWithBackoff {
                    ensureConnected()
                    val share = getOrConnectShare(shareName)
                    val dirPath = path.trimStart('\\', '/')
                    share.list(dirPath)
                        .filter { !it.fileName.startsWith(".") && it.fileName != ".." }
                        .map { it.toSmbFileItem(dirPath) }
                        .sortedWith(compareBy<SmbFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
                }
            }
        }

    suspend fun startStreaming(shareName: String, path: String, fileName: String): Result<Pair<Uri, List<Uri>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureConnected()
                val host = connectedHost ?: error("Not connected")
                val videoUri = Uri.parse("smb://$host/$shareName/$path".replace('\\', '/'))

                val share = getOrConnectShare(shareName)
                val dir = path.substringBeforeLast('\\', "")
                val subtitleUris = share.list(dir)
                    .filter { !it.fileName.startsWith(".") && it.fileName != ".." }
                    .map { it.toSmbFileItem(dir) }
                    .filter { it.isSubtitleFor(fileName.substringBeforeLast('.')) }
                    .map { sub ->
                        Uri.parse("smb://$host/$shareName/${sub.path}".replace('\\', '/'))
                    }

                videoUri to subtitleUris
            }
        }

    @Synchronized
    fun openFileForStreaming(host: String, shareName: String, filePath: String): SmbStreamingFile {
        ensureConnectedToHost(host)
        return withReconnect {
            val share = getOrConnectShare(shareName)
            val file = share.openReadOnly(filePath)
            SmbStreamingFile(file, file.getFileInformation(FileStandardInformation::class.java).endOfFile)
        }
    }

    /** Connects (or reconnects) to [host] using stored credentials when the active session targets a different host. */
    @Synchronized
    private fun ensureConnectedToHost(host: String) {
        if (connectedHost == host && connection?.isConnected == true) return
        runCatching { cachedShare?.close() }
        cachedShare = null
        cachedShareName = null
        runCatching { session?.close() }
        runCatching { connection?.close() }
        session = null
        connection = null
        val creds = runBlocking { serverStore.getCredentials(host) }
            ?.takeIf { it.username.isNotBlank() }
        connectInternal(host, creds)
    }

    fun disconnect() {
        runCatching { cachedShare?.close() }
        cachedShare = null
        cachedShareName = null
        runCatching { session?.close() }
        runCatching { connection?.close() }
        session = null
        connection = null
        connectedHost = null
        connectedCredentials = null
    }

    private fun <T> withReconnect(block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            val host = connectedHost ?: throw e
            runCatching { cachedShare?.close() }
            cachedShare = null
            cachedShareName = null
            runCatching { session?.close() }
            runCatching { connection?.close() }
            connectInternal(host, connectedCredentials)
            block()
        }
    }

    private fun connectInternal(host: String, creds: SmbCredentials?) {
        val conn = client.connect(host)
        connection = conn
        connectedHost = host
        connectedCredentials = creds
        session = if (creds != null) {
            conn.authenticate(AuthenticationContext(creds.username, creds.password.toCharArray(), creds.domain))
        } else {
            conn.authenticate(AuthenticationContext.guest())
        }
    }

    @Synchronized
    private fun getOrConnectShare(shareName: String): DiskShare {
        val existing = cachedShare
        if (existing != null && cachedShareName == shareName && existing.isConnected) {
            return existing
        }
        runCatching { existing?.close() }
        val share = requireSession().connectShare(shareName) as DiskShare
        cachedShare = share
        cachedShareName = shareName
        return share
    }

    private suspend fun ensureConnected() {
        if (connection?.isConnected == true) return
        val host = connectedHost ?: error("Not connected")
        retryWithBackoff { connectInternal(host, connectedCredentials) }
    }

    private fun requireSession(): Session = session ?: error("Not connected")

    private suspend fun <T> retryWithBackoff(block: suspend () -> T): T {
        var delayMs = RETRY_INITIAL_DELAY_MS
        repeat(MAX_RETRIES - 1) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (!e.isSmbRetryable()) throw e
                delay(delayMs)
                delayMs = minOf(delayMs * 2, RETRY_MAX_DELAY_MS)
            }
        }
        return block()
    }

    private fun Throwable.isSmbRetryable(): Boolean {
        val msg = message?.lowercase() ?: ""
        return "access_denied" !in msg && "logon_failure" !in msg &&
            "bad_network_name" !in msg && "bad_network_path" !in msg &&
            "not connected" !in msg
    }

    companion object {
        private const val CONNECTION_TIMEOUT_SEC = 8L
        private const val SO_TIMEOUT_SEC = 12L
        private const val FILE_TIMEOUT_SEC = 30L
        private const val READ_BUFFER_SIZE = 1024 * 1024  // 1 MB per SMB2 READ request
        private const val MAX_RETRIES = 4
        private const val RETRY_INITIAL_DELAY_MS = 500L
        private const val RETRY_MAX_DELAY_MS = 8_000L
    }
}

private fun DiskShare.openReadOnly(path: String) = openFile(
    path,
    EnumSet.of(AccessMask.GENERIC_READ),
    EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
    SMB2CreateDisposition.FILE_OPEN,
    EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
)

private fun FileIdBothDirectoryInformation.toSmbFileItem(parentPath: String): SmbFileItem {
    val isDir = fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
    val fullPath = if (parentPath.isEmpty()) fileName else "$parentPath\\$fileName"
    return SmbFileItem(
        name = fileName,
        path = fullPath,
        isDirectory = isDir,
        size = if (isDir) 0 else endOfFile,
    )
}
