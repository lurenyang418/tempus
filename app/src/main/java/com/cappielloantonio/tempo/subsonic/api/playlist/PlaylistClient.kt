package com.cappielloantonio.tempo.subsonic.api.playlist

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class PlaylistClient(private val subsonic: Subsonic) {
    private val playlistService: PlaylistService

    init {
        this.playlistService =
            RetrofitClient(subsonic).retrofit.create<PlaylistService>(PlaylistService::class.java)
    }

    val playlists: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getPlaylists()")
            return playlistService.getPlaylists(subsonic.params)
        }

    fun getPlaylist(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getPlaylist()")
        return playlistService.getPlaylist(subsonic.params, id)
    }

    fun createPlaylist(
        playlistId: String?,
        name: String?,
        songsId: ArrayList<String?>?
    ): Call<ApiResponse?>? {
        Log.d(TAG, "createPlaylist()")
        return playlistService.createPlaylist(subsonic.params, playlistId, name, songsId)
    }

    fun updatePlaylist(
        playlistId: String?,
        name: String?,
        isPublic: Boolean,
        songIdToAdd: ArrayList<String?>?,
        songIndexToRemove: ArrayList<Int?>?
    ): Call<ApiResponse?>? {
        Log.d(TAG, "updatePlaylist()")
        return playlistService.updatePlaylist(
            subsonic.params,
            playlistId,
            name,
            isPublic,
            songIdToAdd,
            songIndexToRemove
        )
    }

    fun deletePlaylist(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "deletePlaylist()")
        return playlistService.deletePlaylist(subsonic.params, id)
    }

    companion object {
        private const val TAG = "BrowsingClient"
    }
}
