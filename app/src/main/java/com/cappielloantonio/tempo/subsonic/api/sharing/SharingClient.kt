package com.cappielloantonio.tempo.subsonic.api.sharing

import android.util.Log
import com.cappielloantonio.tempo.subsonic.RetrofitClient
import com.cappielloantonio.tempo.subsonic.Subsonic
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call

class SharingClient(private val subsonic: Subsonic) {
    private val sharingService: SharingService

    init {
        this.sharingService =
            RetrofitClient(subsonic).retrofit.create<SharingService>(SharingService::class.java)
    }

    val shares: Call<ApiResponse?>?
        get() {
            Log.d(TAG, "getShares()")
            return sharingService.getShares(subsonic.params)
        }

    fun createShare(id: String?, description: String?, expires: Long?): Call<ApiResponse?>? {
        Log.d(TAG, "createShare()")
        return sharingService.createShare(subsonic.params, id, description, expires)
    }

    fun updateShare(id: String?, description: String?, expires: Long?): Call<ApiResponse?>? {
        Log.d(TAG, "updateShare()")
        return sharingService.updateShare(subsonic.params, id, description, expires)
    }

    fun deleteShare(id: String?): Call<ApiResponse?>? {
        Log.d(TAG, "deleteShare()")
        return sharingService.deleteShare(subsonic.params, id)
    }

    companion object {
        private const val TAG = "BrowsingClient"
    }
}
