package com.cappielloantonio.tempo.subsonic.api.mediaannotation

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class MediaAnnotationClient(private val subsonic: Subsonic) {
    private val mediaAnnotationService: MediaAnnotationService

    init {
        this.mediaAnnotationService =
            RetrofitClient(subsonic).retrofit.create<MediaAnnotationService>(MediaAnnotationService::class.java)
    }

    fun star(id: String?, albumId: String?, artistId: String?): Call<ApiResponse?>? {
        Log.d(TAG, "star()")
        return mediaAnnotationService.star(subsonic.params, id, albumId, artistId)
    }

    fun unstar(id: String?, albumId: String?, artistId: String?): Call<ApiResponse?>? {
        Log.d(TAG, "unstar()")
        return mediaAnnotationService.unstar(subsonic.params, id, albumId, artistId)
    }

    fun setRating(id: String?, rating: Int): Call<ApiResponse?>? {
        Log.d(TAG, "setRating()")
        return mediaAnnotationService.setRating(subsonic.params, id, rating)
    }

    fun scrobble(id: String?, submission: Boolean): Call<ApiResponse?>? {
        Log.d(TAG, "scrobble()")
        return mediaAnnotationService.scrobble(subsonic.params, id, submission)
    }

    companion object {
        private const val TAG = "MediaAnnotationClient"
    }
}
