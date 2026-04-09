package com.cappielloantonio.tempo.subsonic.api.system

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface SystemService {
    @GET("ping")
    fun ping(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?

    @GET("getLicense")
    fun getLicense(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?

    @GET("getOpenSubsonicExtensions")
    fun getOpenSubsonicExtensions(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?
}
