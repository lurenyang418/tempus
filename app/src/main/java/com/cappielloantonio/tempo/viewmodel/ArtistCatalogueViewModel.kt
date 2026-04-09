package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArtistCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val artistList = MutableLiveData<MutableList<ArtistID3?>?>(ArrayList<ArtistID3?>())

    fun getArtistList(): LiveData<MutableList<ArtistID3?>?> {
        return artistList
    }

    fun loadArtists() {
        getSubsonicClientInstance(false)
            .browsingClient!!
            .artists
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artists != null) {
                        val artists: MutableList<ArtistID3?> = ArrayList<ArtistID3?>()

                        for (index in response.body()!!.subsonicResponse.artists!!.indices!!) {
                            artists.addAll(index.artists!!)
                        }

                        artistList.setValue(artists)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }
}
