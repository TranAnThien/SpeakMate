package com.project.speakmate.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class NetworkStatusProvider @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun observeConnected(): Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) { trySend(false); close(); return@callbackFlow }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(false) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            }
        }
        var isRegistered = false
        try {
            val initial = manager.getNetworkCapabilities(manager.activeNetwork)
            trySend(initial?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true && initial?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
            manager.registerDefaultNetworkCallback(callback)
            isRegistered = true
        } catch (_: SecurityException) { trySend(false) }
        catch (_: RuntimeException) { trySend(false) }
        awaitClose {
            if (isRegistered) {
                try { manager.unregisterNetworkCallback(callback) }
                catch (_: IllegalArgumentException) { Unit }
                catch (_: SecurityException) { Unit }
            }
        }
    }.distinctUntilChanged()
}
