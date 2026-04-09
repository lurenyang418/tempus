package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OpenRepository {
    fun getLyricsBySongId(id: String?): MutableLiveData<LyricsList?> {
        val lyricsList = MutableLiveData<LyricsList?>()

        getSubsonicClientInstance(false)
            .openClient!!
            .getLyricsBySongId(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.lyricsList != null) {
                        lyricsList.setValue(response.body()!!.subsonicResponse.lyricsList)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return lyricsList
    }
}
