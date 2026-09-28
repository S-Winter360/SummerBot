package com.example.network

import kotlinx.coroutines.flow.Flow

/**
 * Categorization of physical device network connectivity.
 */
enum class NetworkState(val label: String) {
    OFFLINE("Offline (Isolated)"),
    WIFI("Wi-Fi Connected"),
    CELLULAR("Cellular Connected"),
    ETHERNET("Ethernet Connected"),
    OTHER("Active Connection")
}

/**
 * Provider interface observing network status.
 * Summer uses this to enforce offline-first execution guarantees.
 */
interface NetworkInformationProvider {
    val networkState: Flow<NetworkState>
    val isOnline: Flow<Boolean>

    fun getCurrentState(): NetworkState
}
