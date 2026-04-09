package com.cappielloantonio.tempo.subsonic.api.open

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class OpenClient(private val subsonic: Subsonic) {
    private val openService: OpenService

    init {
        this.openService =
            RetrofitClient(subsonic).retrofit.create<OpenService>(OpenService::class.java)
    }

    fun getLyricsBySongId(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getLyricsBySongId()")
        return openService.getLyricsBySongId(subsonic.params, id)
    }

    companion object {
        private const val TAG = "OpenClient"
    }
}
