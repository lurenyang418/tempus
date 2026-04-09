package com.cappielloantonio.tempo.subsonic.api.mediaretrieval

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class MediaRetrievalClient(private val subsonic: Subsonic) {
    private val mediaRetrievalService: MediaRetrievalService

    init {
        this.mediaRetrievalService =
            RetrofitClient(subsonic).retrofit.create<MediaRetrievalService>(MediaRetrievalService::class.java)
    }

    fun stream(id: String?, maxBitRate: Int?, format: String?): Call<ApiResponse?>? {
        Log.d(TAG, "stream()")
        return mediaRetrievalService.stream(subsonic.params, id, maxBitRate, format)
    }

    fun download(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "download()")
        return mediaRetrievalService.download(subsonic.params, id)
    }

    fun getLyrics(artist: String?, title: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getLyrics()")
        return mediaRetrievalService.getLyrics(subsonic.params, artist, title)
    }

    companion object {
        private const val TAG = "MediaRetrievalClient"
    }
}
