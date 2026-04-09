package com.cappielloantonio.tempo.subsonic.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.cappielloantonio.tempo.App.Companion.getContext
import okhttp3.Interceptor

class CacheUtil(// 60 seconds
    private var maxAge: Int, // 60 * 60 * 24 * 30 = 30 days (60 seconds * 60 minutes * 24 hours * 30 days)
    private var maxStale: Int
) {
    var onlineInterceptor: Interceptor = Interceptor { chain: Interceptor.Chain? ->
        val response = chain!!.proceed(chain.request())
        response.newBuilder()
            .header("Cache-Control", "public, max-age=" + maxAge)
            .removeHeader("Pragma")
            .build()
    }

    var offlineInterceptor: Interceptor = Interceptor { chain: Interceptor.Chain? ->
        var request = chain!!.request()
        if (!this.isConnected) {
            request = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                .removeHeader("Pragma")
                .build()
        }
        chain.proceed(request)
    }


    private val isConnected: Boolean
        get() {
            val connectivityManager = getContext()!!
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager == null) {
                return false
            }

            val network = connectivityManager.getActiveNetwork()
            if (network == null) {
                return false
            }

            val capabilities =
                connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                return false
            }

            val hasInternet =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet) {
                return false
            }

            val hasAppropriateTransport =
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            if (!hasAppropriateTransport) {
                return false
            }

            return true
        }
}
