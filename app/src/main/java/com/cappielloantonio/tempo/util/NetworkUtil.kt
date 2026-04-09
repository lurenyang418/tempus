package com.cappielloantonio.tempo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.cappielloantonio.tempo.App.Companion.getContext

object NetworkUtil {
    val isOffline: Boolean
        get() {
            val connectivityManager = getContext()!!
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

            if (connectivityManager != null) {
                val network = connectivityManager.getActiveNetwork()

                if (network != null) {
                    val capabilities =
                        connectivityManager.getNetworkCapabilities(network)

                    if (capabilities != null) {
                        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) || !capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED
                        )
                    }
                }
            }

            return true
        }
}
