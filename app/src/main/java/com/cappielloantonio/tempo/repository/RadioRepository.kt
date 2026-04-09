package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RadioRepository {
    val internetRadioStations: MutableLiveData<MutableList<InternetRadioStation?>?>
        get() {
            val radioStation =
                MutableLiveData<MutableList<InternetRadioStation?>?>(ArrayList<InternetRadioStation?>())

            getSubsonicClientInstance(false)
                .internetRadioClient!!
                .internetRadioStations
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.internetRadioStations != null && response.body()!!.subsonicResponse.internetRadioStations!!.internetRadioStations != null) {
                            radioStation.setValue(ArrayList(response.body()!!.subsonicResponse.internetRadioStations!!.internetRadioStations!!))
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse?>,
                        t: Throwable
                    ) {
                    }
                })

            return radioStation
        }

    fun createInternetRadioStation(
        name: String?,
        streamURL: String?,
        homepageURL: String?
    ): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .internetRadioClient!!
            .createInternetRadioStation(streamURL, name, homepageURL)
    }

    fun updateInternetRadioStation(
        id: String?,
        name: String?,
        streamURL: String?,
        homepageURL: String?
    ): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .internetRadioClient!!
            .updateInternetRadioStation(id, streamURL, name, homepageURL)
    }

    fun deleteInternetRadioStation(id: String?): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .internetRadioClient!!
            .deleteInternetRadioStation(id)
    }
}
