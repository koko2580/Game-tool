package com.example.data.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.domain.model.HttpLatencyResult
import com.example.domain.model.NetworkLinkTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NetworkTelemetryManager(private val context: Context) {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    fun observeNetworkLink(): Flow<NetworkLinkTelemetry> = callbackFlow {
        trySend(readCurrentNetworkLink())

        val cm = connectivityManager
        if (cm == null) {
            awaitClose {}
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readCurrentNetworkLink())
            }

            override fun onLost(network: Network) {
                trySend(readCurrentNetworkLink())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(readCurrentNetworkLink())
            }
        }

        val registered = runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            true
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }

    fun readCurrentNetworkLink(): NetworkLinkTelemetry {
        val cm = connectivityManager ?: return NetworkLinkTelemetry(
            isConnected = false,
            transportLabel = "Unavailable",
            isValidatedInternet = false,
            isMetered = false,
            downstreamKbps = null,
            upstreamKbps = null
        )

        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        if (caps == null) {
            return NetworkLinkTelemetry(
                isConnected = false,
                transportLabel = "Offline",
                isValidatedInternet = false,
                isMetered = false,
                downstreamKbps = null,
                upstreamKbps = null
            )
        }

        val transports = buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
        }

        val label = if (transports.isEmpty()) "Connected" else transports.joinToString(" + ")
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val down = caps.linkDownstreamBandwidthKbps.takeIf { it > 0 }
        val up = caps.linkUpstreamBandwidthKbps.takeIf { it > 0 }

        return NetworkLinkTelemetry(
            isConnected = true,
            transportLabel = label,
            isValidatedInternet = isValidated,
            isMetered = isMetered,
            downstreamKbps = down,
            upstreamKbps = up
        )
    }

    suspend fun measureHttpLatency(endpointName: String, url: String): HttpLatencyResult =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val request = Request.Builder()
                .url(url)
                .head()
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()

            val startNs = System.nanoTime()
            runCatching {
                httpClient.newCall(request).execute().use { response ->
                    val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                    HttpLatencyResult(
                        endpointName = endpointName,
                        url = url,
                        roundTripMs = elapsedMs,
                        httpCode = response.code,
                        errorMessage = null,
                        timestampMillis = now
                    )
                }
            }.getOrElse { error ->
                HttpLatencyResult(
                    endpointName = endpointName,
                    url = url,
                    roundTripMs = null,
                    httpCode = null,
                    errorMessage = error.localizedMessage ?: "Network probe failed",
                    timestampMillis = now
                )
            }
        }
}
