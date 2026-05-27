package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.interfaces.SystemCallback
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.cappielloantonio.tempo.subsonic.models.ResponseStatus
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SystemRepository {
    fun checkUserCredential(callback: SystemCallback) {
        getSubsonicClientInstance(false)
            .systemClient!!
            .ping()
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.body() != null) {
                        if (response.body()!!.subsonicResponse.status == ResponseStatus.FAILED) {
                            callback.onError(Exception(response.body()!!.subsonicResponse.error!!.code.toString() + " - " + response.body()!!.subsonicResponse.error!!.message))
                        } else if (response.body()!!.subsonicResponse.status == ResponseStatus.OK) {
                            val password = response.raw().request.url.queryParameter("p")
                            val token = response.raw().request.url.queryParameter("t")
                            val salt = response.raw().request.url.queryParameter("s")
                            callback.onSuccess(password, token, salt)
                        } else {
                            callback.onError(Exception("Empty response"))
                        }
                    } else {
                        callback.onError(Exception(response.code().toString()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    callback.onError(Exception(t.message))
                }
            })
    }

    fun ping(): MutableLiveData<SubsonicResponse?> {
        val pingResult = MutableLiveData<SubsonicResponse?>()

        getSubsonicClientInstance(false)
            .systemClient!!
            .ping()
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        pingResult.postValue(response.body()!!.subsonicResponse)
                    } else {
                        pingResult.postValue(null)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    pingResult.postValue(null)
                }
            })

        return pingResult
    }

    val openSubsonicExtensions: MutableLiveData<MutableList<OpenSubsonicExtension?>?>
        get() {
            val extensionsResult =
                MutableLiveData<MutableList<OpenSubsonicExtension?>?>()

            getSubsonicClientInstance(false)
                .systemClient!!
                .openSubsonicExtensions
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            extensionsResult.postValue(ArrayList(response.body()!!.subsonicResponse.openSubsonicExtensions!!))
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse?>,
                        t: Throwable
                    ) {
                        extensionsResult.postValue(null)
                    }
                })

            return extensionsResult
        }

}
