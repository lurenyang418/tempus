package com.cappielloantonio.tempo.subsonic.api.system

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.util.Preferences.getNetworkPingTimeout
import com.cappielloantonio.tempo.util.Preferences.isInUseServerAddressLocal
import retrofit2.Call
import java.util.concurrent.TimeUnit
import kotlin.math.min

class SystemClient(private val subsonic: Subsonic) {
    private val systemService: SystemService

    init {
        this.systemService =
            RetrofitClient(subsonic).retrofit.create<SystemService>(SystemService::class.java)
    }

    fun ping(): Call<ApiResponse?>? {
        Log.d(TAG, "ping()")
        val timeoutSeconds = getNetworkPingTimeout()
        val pingCall = systemService.ping(subsonic.params) ?: return null
        if (isInUseServerAddressLocal()) {
            pingCall.timeout()
                .timeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        } else {
            val finalTimeout = min(timeoutSeconds * 2, 10)
            pingCall.timeout()
                .timeout(finalTimeout.toLong(), TimeUnit.SECONDS)
        }
        return pingCall
    }

    val license: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getLicense()")
            return systemService.getLicense(subsonic.params)
        }

    val openSubsonicExtensions: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getOpenSubsonicExtensions()")
            return systemService.getOpenSubsonicExtensions(subsonic.params)
        }

    companion object {
        private const val TAG = "SystemClient"
    }
}
