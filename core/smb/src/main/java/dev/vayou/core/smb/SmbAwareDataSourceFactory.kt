package dev.vayou.core.smb

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbAwareDataSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smbFactory: SmbDataSourceFactory,
) : DataSource.Factory {

    @OptIn(UnstableApi::class)
    private val defaultFactory = DefaultDataSource.Factory(context)

    @OptIn(UnstableApi::class)
    override fun createDataSource(): DataSource = SmbAwareDataSource(
        defaultDataSource = defaultFactory.createDataSource(),
        smbDataSource = smbFactory.createDataSource(),
    )
}

@OptIn(UnstableApi::class)
class SmbAwareDataSource(
    private val defaultDataSource: DataSource,
    private val smbDataSource: DataSource,
) : DataSource {

    private var activeDataSource: DataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        activeDataSource = if (dataSpec.uri.scheme == "smb") smbDataSource else defaultDataSource
        return activeDataSource!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        activeDataSource!!.read(buffer, offset, length)

    override fun getUri(): Uri? = activeDataSource?.getUri()

    override fun close() {
        activeDataSource?.close()
        activeDataSource = null
    }

    override fun addTransferListener(transferListener: TransferListener) {
        defaultDataSource.addTransferListener(transferListener)
        smbDataSource.addTransferListener(transferListener)
    }
}
