package com.example.potholereport.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.util.concurrent.atomic.AtomicBoolean

/** Lightweight internet reachability for trip / OSRM (GPS itself does not need this). */
object NetworkReachability {

    fun hasInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    /**
     * Calls [onChanged] whenever reachability flips (online or offline).
     * Also fires once with the current state shortly after register.
     * Returns an unregister function.
     */
    fun registerConnectivityListener(
        context: Context,
        onChanged: (online: Boolean) -> Unit,
    ): () -> Unit {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return {}
        val appContext = context.applicationContext
        val lastOnline = AtomicBoolean(hasInternet(appContext))
        onChanged(lastOnline.get())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publish()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) = publish()

            override fun onLost(network: Network) = publish()

            private fun publish() {
                val online = hasInternet(appContext)
                if (lastOnline.getAndSet(online) != online) {
                    onChanged(online)
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            return {}
        }
        return {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Calls [onOnline] when a network with internet becomes available after being offline.
     * Does not fire on the initial online state. Returns an unregister function.
     */
    fun registerOnlineListener(
        context: Context,
        onOnline: () -> Unit,
    ): () -> Unit {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return {}
        val appContext = context.applicationContext
        val wasOffline = AtomicBoolean(!hasInternet(appContext))
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                maybeFire()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                maybeFire()
            }

            override fun onLost(network: Network) {
                wasOffline.set(!hasInternet(appContext))
            }

            private fun maybeFire() {
                if (!hasInternet(appContext)) {
                    wasOffline.set(true)
                    return
                }
                if (wasOffline.compareAndSet(true, false)) {
                    onOnline()
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            return {}
        }
        return {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // Already unregistered / process tearing down.
            }
        }
    }
}
