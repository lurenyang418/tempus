package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.App.Companion.getSubsonicPublicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.Share
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SharingRepository {
    val shares: MutableLiveData<MutableList<Share?>?>
        get() {
            val shares =
                MutableLiveData<MutableList<Share?>?>(ArrayList<Share?>())

            getSubsonicClientInstance(false)
                .sharingClient!!
                .shares
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.shares != null && response.body()!!.subsonicResponse.shares!!.shares != null) {
                            shares.setValue(ArrayList(response.body()!!.subsonicResponse.shares!!.shares!!))
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse?>,
                        t: Throwable
                    ) {
                    }
                })

            return shares
        }

    fun createShare(id: String?, description: String?, expires: Long?): MutableLiveData<Share?> {
        val share = MutableLiveData<Share?>()

        getSubsonicPublicClientInstance(false)
            .sharingClient!!
            .createShare(id, description, expires)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.shares != null && response.body()!!.subsonicResponse.shares!!.shares != null
                    ) {
                        share.setValue(response.body()!!.subsonicResponse.shares!!.shares!!.get(0))
                    } else {
                        share.setValue(null)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    share.setValue(null)
                }
            })

        return share
    }

    fun updateShare(id: String?, description: String?, expires: Long?) {
        getSubsonicPublicClientInstance(false)
            .sharingClient!!
            .updateShare(id, description, expires)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }

    fun deleteShare(id: String?) {
        getSubsonicClientInstance(false)
            .sharingClient!!
            .deleteShare(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }
}
