package com.cappielloantonio.tempo.subsonic.api.playlist

import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface PlaylistService {
    @GET("getPlaylists")
    fun getPlaylists(@QueryMap params: MutableMap<String?, String?>?): Call<ApiResponse?>?

    @GET("getPlaylist")
    fun getPlaylist(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?
    ): Call<ApiResponse?>?

    @GET("createPlaylist")
    fun createPlaylist(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("playlistId") playlistId: String?,
        @Query("name") name: String?,
        @Query("songId") songsId: ArrayList<String?>?
    ): Call<ApiResponse?>?

    @GET("updatePlaylist")
    fun updatePlaylist(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("playlistId") playlistId: String?,
        @Query("name") name: String?,
        @Query("public") isPublic: Boolean,
        @Query("songIdToAdd") songIdToAdd: ArrayList<String?>?,
        @Query("songIndexToRemove") songIndexToRemove: ArrayList<Int?>?
    ): Call<ApiResponse?>?

    @GET("deletePlaylist")
    fun deletePlaylist(
        @QueryMap params: MutableMap<String?, String?>?,
        @Query("id") id: String?
    ): Call<ApiResponse?>?
}
