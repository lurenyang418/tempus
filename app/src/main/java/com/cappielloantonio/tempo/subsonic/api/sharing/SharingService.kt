package com.cappielloantonio.tempo.subsonic.api.sharing

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface SharingService {
    @GET("getShares")
    fun getShares(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?

    @GET("createShare")
    fun createShare(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?,
        @Query("description") description: String?,
        @Query("expires") expires: Long?
    ): Call<ApiResponse?>?

    @GET("updateShare")
    fun updateShare(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?,
        @Query("description") description: String?,
        @Query("expires") expires: Long?
    ): Call<ApiResponse?>?

    @GET("deleteShare")
    fun deleteShare(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?
    ): Call<ApiResponse?>?
}
