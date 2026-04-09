package com.cappielloantonio.tempo.subsonic.api.internetradio

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class InternetRadioClient(private val subsonic: Subsonic) {
    private val internetRadioService: InternetRadioService

    init {
        this.internetRadioService =
            RetrofitClient(subsonic).retrofit.create<InternetRadioService>(InternetRadioService::class.java)
    }

    val internetRadioStations: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getInternetRadioStations()")
            return internetRadioService.getInternetRadioStations(subsonic.params)
        }

    fun createInternetRadioStation(
        streamUrl: String?,
        name: String?,
        homepageUrl: String?
    ): Call<ApiResponse?>? {
        Log.d(TAG, "createInternetRadioStation()")
        return internetRadioService.createInternetRadioStation(
            subsonic.params,
            streamUrl,
            name,
            homepageUrl
        )
    }

    fun updateInternetRadioStation(
        id: String?,
        streamUrl: String?,
        name: String?,
        homepageUrl: String?
    ): Call<ApiResponse?>? {
        Log.d(TAG, "updateInternetRadioStation()")
        return internetRadioService.updateInternetRadioStation(
            subsonic.params,
            id,
            streamUrl,
            name,
            homepageUrl
        )
    }

    fun deleteInternetRadioStation(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "deleteInternetRadioStation()")
        return internetRadioService.deleteInternetRadioStation(subsonic.params, id)
    }

    companion object {
        private const val TAG = "InternetRadioClient"
    }
}
