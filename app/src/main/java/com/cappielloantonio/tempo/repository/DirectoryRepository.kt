package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.Directory
import com.cappielloantonio.tempo.subsonic.models.Indexes
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DirectoryRepository {
    val musicFolders: MutableLiveData<MutableList<MusicFolder?>?>
        get() {
            val liveMusicFolders =
                MutableLiveData<MutableList<MusicFolder?>?>()

            getSubsonicClientInstance(false)
                .browsingClient!!
                .musicFolders
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.musicFolders != null) {
                            liveMusicFolders.setValue(ArrayList(response.body()!!.subsonicResponse.musicFolders!!.musicFolders!!))
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse?>,
                        t: Throwable
                    ) {
                    }
                })

            return liveMusicFolders
        }

    fun getIndexes(musicFolderId: String?, ifModifiedSince: Long?): MutableLiveData<Indexes?> {
        val liveIndexes = MutableLiveData<Indexes?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getIndexes(musicFolderId, ifModifiedSince)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.indexes != null) {
                        liveIndexes.setValue(response.body()!!.subsonicResponse.indexes)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return liveIndexes
    }

    fun getMusicDirectory(id: String?): MutableLiveData<Directory?> {
        val liveMusicDirectory = MutableLiveData<Directory?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getMusicDirectory(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.directory != null) {
                        liveMusicDirectory.setValue(response.body()!!.subsonicResponse.directory)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    t.printStackTrace()
                }
            })

        return liveMusicDirectory
    }

    companion object {
        private const val TAG = "DirectoryRepository"
    }
}
