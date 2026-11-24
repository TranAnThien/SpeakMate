package com.project.speakmate.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

@Singleton
class NetworkStatusProvider @Inject constructor(@param:ApplicationContext private val context: Context) {
    companion object {
        private const val RETRY_BASE_DELAY_MILLIS = 1_000L
        private const val RETRY_MAX_SHORT_DELAY_MILLIS = 16_000L
        private const val RETRY_LONG_DELAY_MILLIS = 60_000L
        private const val RETRY_SHORT_LIMIT = 5
    }

    fun observeConnectedWithRecovery(source: () -> Flow<Boolean> = ::observeConnected): Flow<Boolean> = flow {
        var consecutiveFailures = 0
        val recoveringSource =
            flow {
                emitAll(source())
                throw SourceCompletedException()
            }
        emitAll(
            recoveringSource
                .onEach { consecutiveFailures = 0 }
                .retryWhen { cause, _ ->
                    if (cause is CancellationException || cause !is Exception) return@retryWhen false
                    consecutiveFailures++
                    emit(false)
                    delay(getRetryDelayMillis(consecutiveFailures))
                    true
                }
        )
    }

    private fun getRetryDelayMillis(failures: Int): Long =
        if (failures <= RETRY_SHORT_LIMIT) {
            (RETRY_BASE_DELAY_MILLIS shl (failures - 1)).coerceAtMost(RETRY_MAX_SHORT_DELAY_MILLIS)
        } else {
            RETRY_LONG_DELAY_MILLIS
        }

    fun isConnected(): Boolean =
        try {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: RuntimeException) {
            false
        }

    fun observeConnected(): Flow<Boolean> =
        callbackFlow {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            if (manager == null) {
                trySend(element = false)
                close()
                return@callbackFlow
            }
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(element = false)
                    }

                    override fun onLost(network: Network) {
                        trySend(element = false)
                    }

                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        trySend(
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        )
                    }
                }
            var isRegistered = false
            try {
                val initial = manager.getNetworkCapabilities(manager.activeNetwork)
                trySend(
                    initial?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                        initial?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                )
                manager.registerDefaultNetworkCallback(callback)
                isRegistered = true
            } catch (_: SecurityException) {
                trySend(element = false)
            } catch (_: RuntimeException) {
                trySend(element = false)
            }
            awaitClose {
                if (isRegistered) {
                    try {
                        manager.unregisterNetworkCallback(callback)
                    } catch (_: IllegalArgumentException) {
                        Unit
                    } catch (_: SecurityException) {
                        Unit
                    }
                }
            }
        }
            .distinctUntilChanged()

    private class SourceCompletedException : Exception()
}
