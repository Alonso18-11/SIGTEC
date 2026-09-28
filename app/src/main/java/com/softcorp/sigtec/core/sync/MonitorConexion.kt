package com.softcorp.sigtec.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorConexion @Inject constructor(@ApplicationContext context: Context) {

    private val conectividad = context.getSystemService(ConnectivityManager::class.java)

    // VALIDATED: hay internet de verdad, no solo una red Wi-Fi sin salida
    private fun conexionActual(): Boolean =
        conectividad.getNetworkCapabilities(conectividad.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

    val hayConexion: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(red: Network, capacidades: NetworkCapabilities) {
                trySend(capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            }

            override fun onLost(red: Network) {
                trySend(conexionActual())
            }
        }
        trySend(conexionActual())
        conectividad.registerDefaultNetworkCallback(callback)
        awaitClose { conectividad.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
