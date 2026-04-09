package com.cappielloantonio.tempo.subsonic.api.searching

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class SearchingClient(private val subsonic: Subsonic) {
    private val searchingService: SearchingService

    init {
        this.searchingService =
            RetrofitClient(subsonic).retrofit.create<SearchingService>(SearchingService::class.java)
    }

    fun search2(
        query: String?,
        songCount: Int,
        albumCount: Int,
        artistCount: Int
    ): Call<ApiResponse?>? {
        Log.d(TAG, "search2()")
        return searchingService.search2(subsonic.params, query, songCount, albumCount, artistCount)
    }

    fun search3(
        query: String?,
        songCount: Int,
        songOffset: Int,
        albumCount: Int,
        albumOffset: Int,
        artistCount: Int,
        artistOffset: Int
    ): Call<ApiResponse?>? {
        Log.d(TAG, "search3()")
        return searchingService.search3(
            subsonic.params,
            query,
            songCount,
            songOffset,
            albumCount,
            albumOffset,
            artistCount,
            artistOffset
        )
    }

    companion object {
        private const val TAG = "BrowsingClient"
    }
}
