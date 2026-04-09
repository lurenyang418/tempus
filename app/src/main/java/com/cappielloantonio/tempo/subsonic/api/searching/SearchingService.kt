package com.cappielloantonio.tempo.subsonic.api.searching

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface SearchingService {
    @GET("search2")
    fun search2(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("query") query: String?,
        @Query("songCount") songCount: Int,
        @Query("albumCount") albumCount: Int,
        @Query("artistCount") artistCount: Int
    ): Call<ApiResponse?>?

    @GET("search3")
    fun search3(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("query") query: String?,
        @Query("songCount") songCount: Int,
        @Query("songOffset") songOffset: Int,
        @Query("albumCount") albumCount: Int,
        @Query("albumOffset") albumOffset: Int,
        @Query("artistCount") artistCount: Int,
        @Query("artistOffset") artistOffset: Int
    ): Call<ApiResponse?>?
}
