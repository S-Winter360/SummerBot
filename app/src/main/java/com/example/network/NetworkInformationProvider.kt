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

interface NetworkInformationProvider {
    val networkState: Flow<NetworkState>
    val isOnline: Flow<Boolean>
    fun getCurrentState(): NetworkState
}

class AndroidNetworkInformationProvider(
    private val context: Context
) : NetworkInformationProvider {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val networkState: Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(resolveCurrentState())
            }

            override fun onLost(network: Network) {
                trySend(resolveCurrentState())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(resolveCurrentState())
            }
        }

        trySend(resolveCurrentState())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, callback)

        awaitClose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    override val isOnline: Flow<Boolean> = networkState.map { it != NetworkState.OFFLINE }

    override fun getCurrentState(): NetworkState = resolveCurrentState()

    private fun resolveCurrentState(): NetworkState {
        val cm = connectivityManager ?: return NetworkState.OFFLINE
        val activeNetwork = cm.activeNetwork ?: return NetworkState.OFFLINE
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkState.OFFLINE

        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkState.OFFLINE
        }

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.CONNECTED_WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.CONNECTED_CELLULAR
            else -> NetworkState.CONNECTED_OTHER
        }
    }
}
