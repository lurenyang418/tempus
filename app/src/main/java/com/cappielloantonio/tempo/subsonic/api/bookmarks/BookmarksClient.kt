package com.cappielloantonio.tempo.subsonic.api.bookmarks

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class BookmarksClient(private val subsonic: Subsonic) {
    private val bookmarksService: BookmarksService

    init {
        this.bookmarksService =
            RetrofitClient(subsonic).retrofit.create<BookmarksService>(BookmarksService::class.java)
    }

    val playQueue: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getPlayQueue()")
            return bookmarksService.getPlayQueue(subsonic.params)
        }

    fun savePlayQueue(
        ids: MutableList<String?>?,
        current: String?,
        position: Long
    ): Call<ApiResponse?>? {
        Log.d(TAG, "savePlayQueue()")
        return bookmarksService.savePlayQueue(subsonic.params, ids, current, position)
    }

    companion object {
        private const val TAG = "BookmarksClient"
    }
}
