package dev.vayou.core.smb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * What is sharing files on this network.
 *
 * Two ways at once, because neither finds everything. A NAS announces itself over mDNS and is found
 * in a second; an old Windows box or a hand-configured Samba announces nothing at all and is only
 * found by knocking on every address. Run in parallel, the announcing machines appear immediately
 * and the quiet ones follow.
 */
@Singleton
class SmbDiscovery @Inject constructor(@ApplicationContext private val context: Context) {

    /** Servers as they turn up, deduplicated by address, the list growing with each find. */
    fun discover(): Flow<List<SmbServer>> = channelFlow {
        val found = mutableMapOf<String, SmbServer>()
        val lock = Mutex()

        suspend fun report(server: SmbServer) {
            val isNew = lock.withLock { found.put(server.host, server) == null }
            if (isNew) send(lock.withLock { found.values.sortedBy { it.displayName } })
        }

        val announced = launch { announcedServers().collect(::report) }
        val knocked = launch { serversOnSubnet().collect(::report) }
        announced.join()
        knocked.join()
    }.flowOn(Dispatchers.IO)

    suspend fun isReachable(host: String): Boolean = withContext(Dispatchers.IO) { respondsToSmb(host) }

    /**
     * The machines that advertise `_smb._tcp` -- Synology, QNAP, TrueNAS, Windows 10 and 11.
     *
     * Ends after [MdnsWindowMs] rather than running on: this feeds a list the viewer is looking at,
     * and a scan with no end is a spinner with no end.
     */
    private fun announcedServers(): Flow<SmbServer> = channelFlow {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return@channelFlow
        // NsdManager resolves one service at a time below API 34, and asking for a second while the
        // first is running fails both.
        val oneAtATime = Mutex()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                launch {
                    oneAtATime.withLock {
                        resolve(nsd, serviceInfo)?.let { trySend(it) }
                    }
                }
            }
        }

        nsd.discoverServices(SmbServiceType, NsdManager.PROTOCOL_DNS_SD, listener)
        delay(MdnsWindowMs)
        runCatching { nsd.stopServiceDiscovery(listener) }
        close()
    }

    private suspend fun resolve(nsd: NsdManager, serviceInfo: NsdServiceInfo): SmbServer? =
        suspendCancellableCoroutine { continuation ->
            @Suppress("DEPRECATION")
            nsd.resolveService(
                serviceInfo,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = continuation.resume(null)

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        val host = info.host?.hostAddress
                        continuation.resume(host?.let { SmbServer(it, info.serviceName ?: it) })
                    }
                },
            )
        }

    /** Every address on this subnet, knocked on at once. Catches what does not advertise itself. */
    private fun serversOnSubnet(): Flow<SmbServer> = channelFlow {
        val subnet = localSubnet() ?: return@channelFlow
        (1..LastHostOctet).map { octet ->
            async(Dispatchers.IO) {
                val host = "$subnet.$octet"
                if (respondsToSmb(host)) {
                    val name = runCatching {
                        InetAddress.getByName(host).hostName.takeIf { it != host } ?: host
                    }.getOrDefault(host)
                    send(SmbServer(host, name))
                }
            }
        }.awaitAll()
    }

    private fun localSubnet(): String? = try {
        @Suppress("DEPRECATION")
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        @Suppress("DEPRECATION")
        val address = wifi?.connectionInfo?.ipAddress?.takeIf { it != 0 }
        address?.let { "${it and 0xFF}.${(it shr 8) and 0xFF}.${(it shr 16) and 0xFF}" }
    } catch (_: SecurityException) {
        null
    }

    private fun respondsToSmb(host: String): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, SmbPort), KnockTimeoutMs) }
        true
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val SmbServiceType = "_smb._tcp"
        const val SmbPort = 445

        /** Half a second. A machine on the same Wi-Fi answers in tens of milliseconds or not at all. */
        const val KnockTimeoutMs = 500

        const val MdnsWindowMs = 3_000L

        /** .255 is the broadcast address and .0 is the network itself. */
        const val LastHostOctet = 254
    }
}
