package com.cappielloantonio.tempo.subsonic.api.medialibraryscanning

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class MediaLibraryScanningClient(private val subsonic: Subsonic) {
    private val mediaLibraryScanningService: MediaLibraryScanningService

    init {
        this.mediaLibraryScanningService =
            RetrofitClient(subsonic).retrofit.create<MediaLibraryScanningService>(
                MediaLibraryScanningService::class.java
            )
    }

    fun startScan(): Call<ApiResponse?>? {
        Log.d(TAG, "startScan()")
        return mediaLibraryScanningService.startScan(subsonic.params)
    }

    val scanStatus: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getScanStatus()")
            return mediaLibraryScanningService.getScanStatus(subsonic.params)
        }

    companion object {
        private const val TAG = "MediaLibraryScanningClient"
    }
}
