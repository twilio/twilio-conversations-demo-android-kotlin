package com.twilio.conversations.app.manager

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ConnectivityMonitor {
    val isNetworkAvailable: StateFlow<Boolean>
}

class ConnectivityMonitorImpl(context: Context) : ConnectivityMonitor {

    private val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isNetworkAvailable = MutableStateFlow(getInitialConnectionStatus())

    init {
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), ConnectionStatusCallback())
    }

    @Suppress("DEPRECATION")
    private fun getInitialConnectionStatus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NET_CAPABILITY_INTERNET) ?: false
        } else {
            val activeNetwork = connectivityManager.activeNetworkInfo // Deprecated in 29
            activeNetwork != null && activeNetwork.isConnectedOrConnecting // // Deprecated in 28
        }
    }

    private inner class ConnectionStatusCallback : ConnectivityManager.NetworkCallback() {

        private val activeNetworks: MutableList<Network> = mutableListOf()

        override fun onLost(network: Network) {
            activeNetworks.removeAll { activeNetwork -> activeNetwork == network }
            isNetworkAvailable.value = activeNetworks.isNotEmpty()
        }

        override fun onAvailable(network: Network) {
            if (activeNetworks.none { activeNetwork -> activeNetwork == network }) {
                activeNetworks.add(network)
            }
            isNetworkAvailable.value = activeNetworks.isNotEmpty()
        }
    }
}
