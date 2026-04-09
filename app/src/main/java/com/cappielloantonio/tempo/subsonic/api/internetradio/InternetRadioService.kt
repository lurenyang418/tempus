package com.cappielloantonio.tempo.subsonic.api.internetradio

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface InternetRadioService {
    @GET("getInternetRadioStations")
    fun getInternetRadioStations(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?

    @GET("createInternetRadioStation")
    fun createInternetRadioStation(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("streamUrl") streamUrl: String?,
        @Query("name") name: String?,
        @Query("homepageUrl") homepageUrl: String?
    ): Call<ApiResponse?>?

    @GET("updateInternetRadioStation")
    fun updateInternetRadioStation(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?,
        @Query("streamUrl") streamUrl: String?,
        @Query("name") name: String?,
        @Query("homepageUrl") homepageUrl: String?
    ): Call<ApiResponse?>?

    @GET("deleteInternetRadioStation")
    fun deleteInternetRadioStation(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?
    ): Call<ApiResponse?>?
}
