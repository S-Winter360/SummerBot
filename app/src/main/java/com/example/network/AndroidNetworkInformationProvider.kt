package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Concrete Android implementation using system ConnectivityManager.
 * Streams genuine device network connectivity state.
 */
class AndroidNetworkInformationProvider(
    private val context: Context
) : NetworkInformationProvider {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val networkState: Flow<NetworkState> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(NetworkState.OFFLINE)
            close()
            return@callbackFlow
        }

        // Send initial state immediately
        trySend(determineCurrentState())

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(determineCurrentState())
            }

            override fun onLost(network: Network) {
                trySend(determineCurrentState())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(determineCurrentState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            trySend(NetworkState.OFFLINE)
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
                // Ignore if already unregistered
            }
        }
    }.distinctUntilChanged()

    override val isOnline: Flow<Boolean> = networkState.map { it != NetworkState.OFFLINE }

    override fun getCurrentState(): NetworkState = determineCurrentState()

    private fun determineCurrentState(): NetworkState {
        val cm = connectivityManager ?: return NetworkState.OFFLINE
        val activeNetwork = cm.activeNetwork ?: return NetworkState.OFFLINE
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkState.OFFLINE

        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkState.OFFLINE
        }

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkState.ETHERNET
            else -> NetworkState.OTHER
        }
    }
}
