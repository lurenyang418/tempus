package com.cappielloantonio.tempo.subsonic.api.browsing

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class BrowsingClient(private val subsonic: Subsonic) {
    private val browsingService: BrowsingService

    init {
        this.browsingService =
            RetrofitClient(subsonic).retrofit.create<BrowsingService>(BrowsingService::class.java)
    }

    val musicFolders: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getMusicFolders()")
            return browsingService.getMusicFolders(subsonic.params)
        }

    fun getIndexes(musicFolderId: String?, ifModifiedSince: Long?): Call<ApiResponse?>? {
        Log.d(TAG, "getIndexes()")
        return browsingService.getIndexes(subsonic.params, musicFolderId, ifModifiedSince)
    }

    fun getMusicDirectory(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getMusicDirectory()")
        return browsingService.getMusicDirectory(subsonic.params, id)
    }

    val genres: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getGenres()")
            return browsingService.getGenres(subsonic.params)
        }

    val artists: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getArtists()")
            return browsingService.getArtists(subsonic.params)
        }

    fun getArtist(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getArtist()")
        return browsingService.getArtist(subsonic.params, id)
    }

    fun getAlbum(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getAlbum()")
        return browsingService.getAlbum(subsonic.params, id)
    }

    fun getSong(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getSong()")
        return browsingService.getSong(subsonic.params, id)
    }

    val videos: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getVideos()")
            return browsingService.getVideos(subsonic.params)
        }

    fun getVideoInfo(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getVideoInfo()")
        return browsingService.getVideoInfo(subsonic.params, id)
    }

    fun getArtistInfo(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getArtistInfo()")
        return browsingService.getArtistInfo(subsonic.params, id)
    }

    fun getArtistInfo2(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getArtistInfo2()")
        return browsingService.getArtistInfo2(subsonic.params, id)
    }

    fun getAlbumInfo(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getAlbumInfo()")
        return browsingService.getAlbumInfo(subsonic.params, id)
    }

    fun getAlbumInfo2(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "getAlbumInfo2()")
        return browsingService.getAlbumInfo2(subsonic.params, id)
    }

    fun getSimilarSongs(id: String?, count: Int): Call<ApiResponse?>? {
        Log.d(TAG, "getSimilarSongs()")
        return browsingService.getSimilarSongs(subsonic.params, id, count)
    }

    fun getSimilarSongs2(id: String?, limit: Int): Call<ApiResponse?>? {
        Log.d(TAG, "getSimilarSongs2()")
        return browsingService.getSimilarSongs2(subsonic.params, id, limit)
    }

    fun getTopSongs(artist: String?, count: Int): Call<ApiResponse?>? {
        Log.d(TAG, "getTopSongs()")
        return browsingService.getTopSongs(subsonic.params, artist, count)
    }

    companion object {
        private const val TAG = "BrowsingClient"
    }
}
