package com.example.network

import kotlinx.coroutines.flow.Flow

interface NetworkInformationProvider {
    val networkState: Flow<NetworkState>
    val isOnline: Flow<Boolean>
    fun getCurrentState(): NetworkState
}
