package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.interfaces.MediaCallback
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlbumCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val albumList = MutableLiveData<MutableList<AlbumID3?>?>(ArrayList<AlbumID3?>())
    private val loading = MutableLiveData<Boolean?>(true)

    private var page = 0
    private var status = Status.STOPPED

    fun getAlbumList(): LiveData<MutableList<AlbumID3?>?> {
        return albumList
    }

    val loadingStatus: LiveData<Boolean?>
        get() = loading

    fun loadAlbums() {
        page = 0
        status = Status.RUNNING
        albumList.setValue(ArrayList<AlbumID3?>())
        loadAlbums(500)
    }

    fun stopLoading() {
        status = Status.STOPPED
    }

    private fun loadAlbums(size: Int) {
        retrieveAlbums(object : MediaCallback {
            override fun onError(exception: Exception?) {
            }

            override fun onLoadMedia(media: MutableList<*>?) {
                if (status == Status.STOPPED) {
                    loading.setValue(false)
                    return
                }

                val liveAlbum = albumList.getValue()
                val albums = media?.filterIsInstance<AlbumID3?>() ?: emptyList()

                liveAlbum!!.addAll(albums)
                albumList.setValue(liveAlbum)

                if (albums.size == size) {
                    loadAlbums(size)
                    loading.setValue(true)
                } else {
                    status = Status.STOPPED
                    loading.setValue(false)
                }
            }
        }, size, size * page++)
    }


    private fun retrieveAlbums(callback: MediaCallback, size: Int, offset: Int) {
        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .getAlbumList2("alphabeticalByName", size, offset, null, null)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.albumList2 != null && response.body()!!.subsonicResponse.albumList2!!.albums != null) {
                        callback.onLoadMedia(response.body()!!.subsonicResponse.albumList2!!.albums?.toMutableList())
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    callback.onError(Exception(t.message))
                }
            })
    }

    private enum class Status {
        RUNNING,
        STOPPED
    }
}