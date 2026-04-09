package com.cappielloantonio.tempo.repository

import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScanRepository {
    fun startScan(callback: ScanCallback) {
        getSubsonicClientInstance(false)
            .mediaLibraryScanningClient!!
            .startScan()
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse != null) {
                        if (response.body()!!.subsonicResponse.error != null) {
                            callback.onError(Exception(response.body()!!.subsonicResponse.error!!.message))
                        } else if (response.body()!!.subsonicResponse.scanStatus != null) {
                            callback.onSuccess(
                                response.body()!!.subsonicResponse.scanStatus!!.isScanning,
                                response.body()!!.subsonicResponse.scanStatus!!.count!!
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    callback.onError(Exception(t.message))
                }
            })
    }

    fun getScanStatus(callback: ScanCallback) {
        getSubsonicClientInstance(false)
            .mediaLibraryScanningClient!!
            .startScan()
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse != null) {
                        if (response.body()!!.subsonicResponse.error != null) {
                            callback.onError(Exception(response.body()!!.subsonicResponse.error!!.message))
                        } else if (response.body()!!.subsonicResponse.scanStatus != null) {
                            callback.onSuccess(
                                response.body()!!.subsonicResponse.scanStatus!!.isScanning,
                                response.body()!!.subsonicResponse.scanStatus!!.count!!
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    callback.onError(Exception(t.message))
                }
            })
    }
}
