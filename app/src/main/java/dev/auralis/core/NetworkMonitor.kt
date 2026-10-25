package dev.auralis.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the phone currently has a usable internet connection.
 * Everything online in Auralis is optional: when this is false the app
 * simply keeps working with the local library.
 */
class NetworkMonitor(context: Context) {

    private val manager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _online = MutableStateFlow(isConnected())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        try {
            manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _online.value = isConnected()
                }

                override fun onLost(network: Network) {
                    _online.value = isConnected()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    _online.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            })
        } catch (_: Throwable) {
            // Some OEMs restrict callbacks; the value is refreshed on each request too.
        }
    }

    fun isConnected(): Boolean {
        val cm = manager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun refresh(): Boolean {
        val value = isConnected()
        _online.value = value
        return value
    }
}
