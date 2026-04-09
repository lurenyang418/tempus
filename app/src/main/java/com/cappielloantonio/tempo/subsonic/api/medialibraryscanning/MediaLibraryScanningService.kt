package com.cappielloantonio.tempo.subsonic.api.medialibraryscanning

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface MediaLibraryScanningService {
    @GET("startScan")
    fun startScan(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?

    @GET("getScanStatus")
    fun getScanStatus(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?
}
