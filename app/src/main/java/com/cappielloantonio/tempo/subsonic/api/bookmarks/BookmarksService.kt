package com.cappielloantonio.tempo.subsonic.api.bookmarks

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface BookmarksService {
    @GET("getPlayQueue")
    fun getPlayQueue(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?

    @GET("savePlayQueue")
    fun savePlayQueue(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") ids: MutableList<String?>?,
        @Query("current") current: String?,
        @Query("position") position: Long
    ): Call<ApiResponse?>?
}
