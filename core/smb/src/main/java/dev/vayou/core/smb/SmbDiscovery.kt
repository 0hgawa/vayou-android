package dev.vayou.core.smb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SmbDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Runs mDNS and subnet scan in parallel.
     * Results are deduplicated by host IP and emitted progressively.
     */
    fun discoverServers(): Flow<List<SmbServer>> = channelFlow {
        val found = mutableMapOf<String, SmbServer>()
        val lock = Mutex()

        suspend fun onFound(server: SmbServer) {
            val isNew = lock.withLock { found.put(server.host, server) == null }
            if (isNew) send(lock.withLock { found.values.sortedBy { it.displayName } })
        }

        val mDnsJob = launch { mDnsDiscover().collect { onFound(it) } }
        val subnetJob = launch { subnetDiscover().collect { onFound(it) } }

        mDnsJob.join()
        subnetJob.join()
    }.flowOn(Dispatchers.IO)

    suspend fun isReachable(host: String): Boolean = withContext(Dispatchers.IO) {
        isSmbReachable(host)
    }

    /**
     * mDNS discovery via Android NsdManager — finds SMB servers that advertise
     * themselves via _smb._tcp.local (Synology, QNAP, TrueNAS, Windows 10/11, etc.)
     * Completes after MDNS_TIMEOUT_MS to allow subnet scan to continue.
     */
    private fun mDnsDiscover(): Flow<SmbServer> = channelFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            ?: return@channelFlow
        val resolveMutex = Mutex()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { close() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                launch {
                    // NsdManager only allows one resolve at a time on API < 34
                    resolveMutex.withLock {
                        val server = resolveService(nsdManager, serviceInfo)
                        if (server != null) trySend(server)
                    }
                }
            }
        }

        nsdManager.discoverServices("_smb._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        delay(MDNS_TIMEOUT_MS)
        runCatching { nsdManager.stopServiceDiscovery(listener) }
        close()
    }

    private suspend fun resolveService(
        nsdManager: NsdManager,
        serviceInfo: NsdServiceInfo,
    ): SmbServer? = suspendCancellableCoroutine { cont ->
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {
                cont.resume(null)
            }

            override fun onServiceResolved(si: NsdServiceInfo) {
                val host = si.host?.hostAddress
                cont.resume(
                    if (host != null) SmbServer(host = host, displayName = si.serviceName ?: host)
                    else null,
                )
            }
        })
    }

    /**
     * Subnet scan via TCP port 445 — catches devices that don't advertise via mDNS
     * (older Windows, manually configured Samba, some routers).
     */
    private fun subnetDiscover(): Flow<SmbServer> = channelFlow {
        val subnet = localSubnet() ?: return@channelFlow
        (1..254).map { i ->
            async(Dispatchers.IO) {
                val host = "$subnet.$i"
                if (isSmbReachable(host)) {
                    val name = runCatching {
                        InetAddress.getByName(host).hostName.takeIf { it != host } ?: host
                    }.getOrDefault(host)
                    send(SmbServer(host = host, displayName = name))
                }
            }
        }.awaitAll()
    }

    private fun localSubnet(): String? = try {
        @Suppress("DEPRECATION")
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        @Suppress("DEPRECATION")
        val ip = wifi.connectionInfo?.ipAddress?.takeIf { it != 0 } ?: return null
        val a = ip and 0xFF
        val b = (ip shr 8) and 0xFF
        val c = (ip shr 16) and 0xFF
        "$a.$b.$c"
    } catch (_: SecurityException) {
        null
    }

    private fun isSmbReachable(host: String): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, SMB_PORT), CONNECT_TIMEOUT_MS) }
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val SMB_PORT = 445
        private const val CONNECT_TIMEOUT_MS = 500
        private const val MDNS_TIMEOUT_MS = 3_000L
    }
}
