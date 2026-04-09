package com.cappielloantonio.tempo.subsonic.api.open

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface OpenService {
    @GET("getLyricsBySongId")
    fun getLyricsBySongId(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?
    ): Call<ApiResponse?>?
}
