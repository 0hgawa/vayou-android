package dev.vayou.core.smb

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A file on a share, read the way ExoPlayer reads anything else.
 *
 * Buffered a megabyte at a time. The player asks for a few kilobytes at a go, and every one of
 * those over the wire would be a round trip; a chunk that size covers most of a demuxer's reads and
 * turns a hundred requests into one.
 */
@OptIn(UnstableApi::class)
class SmbDataSource(private val client: SmbClient) : BaseDataSource(IsNetwork) {

    private var uri: Uri? = null
    private var file: com.hierynomus.smbj.share.File? = null

    /** Kept so a handle lost mid-film can be asked for again. */
    private var host = ""
    private var shareName = ""
    private var filePath = ""

    private var fileSize = 0L
    private var position = 0L
    private var bytesRemaining = 0L

    private val buffer = ByteArray(BufferSize)
    private var bufferStart = -1L
    private var bufferFilled = 0

    override fun open(dataSpec: DataSpec): Long {
        val host = dataSpec.uri.host ?: throw IOException("Not an SMB address: ${dataSpec.uri}")
        val (shareName, filePath) = dataSpec.uri.smbSharePath()

        runCatching { file?.close() }

        this.host = host
        this.shareName = shareName
        this.filePath = filePath

        val opened = client.openForStreaming(host, shareName, filePath)
        uri = dataSpec.uri
        file = opened.file
        fileSize = opened.size

        position = dataSpec.position
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) fileSize - position else dataSpec.length

        bufferStart = -1L
        bufferFilled = 0

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(output: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val wanted = minOf(length.toLong(), bytesRemaining).toInt()

        readFromBuffer(wanted, output, offset)?.let { served ->
            advance(served)
            return served
        }

        val fetched = fetch(minOf(BufferSize.toLong(), fileSize - position).toInt())
        if (fetched <= 0) return C.RESULT_END_OF_INPUT

        bufferStart = position
        bufferFilled = fetched

        val served = minOf(wanted, fetched)
        buffer.copyInto(output, offset, 0, served)
        advance(served)
        return served
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        runCatching { file?.close() }
        file = null
        uri = null
        bytesRemaining = 0L
        bufferStart = -1L
        bufferFilled = 0
        transferEnded()
    }

    /**
     * The next chunk from the share, asking for the file again if the handle has gone.
     *
     * It goes often. A server drops a session it thinks is idle -- and one is, between two reads of
     * a film that is paused, or over the seconds a codec spends on a chapter already buffered -- and
     * says nothing until the next read, which fails. Reopening costs a round trip and the viewer
     * sees nothing; not reopening ends the film with a message about a byte offset.
     *
     * Once, because a second failure is not a dropped session but a server that has gone away, and
     * the player has its own retries above this one.
     */
    private fun fetch(length: Int): Int {
        val open = file ?: throw IOException("Read before open")
        return try {
            open.read(buffer, position, 0, length)
        } catch (dropped: Exception) {
            try {
                runCatching { open.close() }
                client.openForStreaming(host, shareName, filePath).file
                    .also { file = it }
                    .read(buffer, position, 0, length)
            } catch (e: Exception) {
                e.addSuppressed(dropped)
                throw IOException("SMB read failed at $position", e)
            }
        }
    }

    /** What of the request the last chunk already covers, or null when it covers none of it. */
    private fun readFromBuffer(wanted: Int, destination: ByteArray, destinationOffset: Int): Int? {
        if (bufferFilled <= 0 || position < bufferStart || position >= bufferStart + bufferFilled) return null
        val start = (position - bufferStart).toInt()
        val available = minOf(wanted, bufferFilled - start)
        buffer.copyInto(destination, destinationOffset, start, start + available)
        return available
    }

    private fun advance(bytes: Int) {
        position += bytes
        bytesRemaining -= bytes
        bytesTransferred(bytes)
    }

    private companion object {
        const val BufferSize = 1024 * 1024
    }
}

/** A share is across a wire, so the player counts its reads against the network budget. */
private const val IsNetwork = true

/**
 * Whichever source the address calls for.
 *
 * One factory rather than two, because the player is given a single one at construction and the
 * queue behind it can hold a file on disk, one on a share and a channel over HTTP at the same time.
 */
@Singleton
class SmbAwareDataSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: SmbClient,
) : DataSource.Factory {

    @OptIn(UnstableApi::class)
    private val defaultFactory = DefaultDataSource.Factory(context)

    @OptIn(UnstableApi::class)
    override fun createDataSource(): DataSource = SmbAwareDataSource(
        default = defaultFactory.createDataSource(),
        smb = SmbDataSource(client),
    )
}

@OptIn(UnstableApi::class)
private class SmbAwareDataSource(private val default: DataSource, private val smb: DataSource) : DataSource {

    private var active: DataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        val chosen = if (dataSpec.uri.scheme == SmbScheme) smb else default
        active = chosen
        return chosen.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = requireActive().read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun close() {
        active?.close()
        active = null
    }

    override fun addTransferListener(transferListener: TransferListener) {
        default.addTransferListener(transferListener)
        smb.addTransferListener(transferListener)
    }

    private fun requireActive(): DataSource = active ?: throw IOException("Read before open")
}
