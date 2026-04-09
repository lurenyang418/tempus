package com.cappielloantonio.tempo.subsonic.api.mediaannotation

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MediaAnnotationService {
    @GET("star")
    fun star(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?,
        @Query("albumId") albumId: String?,
        @Query("artistId") artistId: String?
    ): Call<ApiResponse?>?

    @GET("unstar")
    fun unstar(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?,
        @Query("albumId") albumId: String?,
        @Query("artistId") artistId: String?
    ): Call<ApiResponse?>?

    @GET("setRating")
    fun setRating(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?,
        @Query("rating") rating: Int
    ): Call<ApiResponse?>?

    @GET("scrobble")
    fun scrobble(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?,
        @Query("submission") submission: Boolean?
    ): Call<ApiResponse?>?
}
