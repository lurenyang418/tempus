package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.RadioRepository
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RadioEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val radioRepository: RadioRepository
    var radioToEdit: InternetRadioStation? = null

    private val isSuccess = MutableLiveData<Boolean?>(false)
    private val errorMessage = MutableLiveData<String?>()

    init {
        radioRepository = RadioRepository()
    }


    fun getIsSuccess(): LiveData<Boolean?> {
        return isSuccess
    }

    fun getErrorMessage(): LiveData<String?> {
        return errorMessage
    }

    fun clearError() {
        errorMessage.setValue(null)
    }

    fun createRadio(name: String?, streamURL: String?, homepageURL: String?) {
        errorMessage.setValue(null)
        isSuccess.setValue(false)

        radioRepository.createInternetRadioStation(name, streamURL, homepageURL)!!
            .enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
                    // Handle HTTP 501 (Not Implemented) from Navidrome
                    if (response.code() == 501) {
                        showError(getApplication<Application>().getString(R.string.radio_dialog_not_supported_snackbar))
                        return
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        val apiResponse: ApiResponse = response.body()!!
                        val status = apiResponse.subsonicResponse.status
                        if ("ok" == status) {
                            isSuccess.setValue(true)
                        } else if ("failed" == status) {
                            handleFailedResponse(apiResponse)
                        }
                    } else {
                        errorMessage.setValue("HTTP error: " + response.code())
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    errorMessage.setValue("Network error: " + t.message)
                }
            })
    }

    fun updateRadio(name: String?, streamURL: String?, homepageURL: String?) {
        if (this.radioToEdit != null && radioToEdit!!.id != null) {
            errorMessage.setValue(null)
            isSuccess.setValue(false)

            radioRepository.updateInternetRadioStation(
                radioToEdit!!.id,
                name,
                streamURL,
                homepageURL
            )!!
                .enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            val apiResponse: ApiResponse = response.body()!!
                            if (apiResponse.subsonicResponse != null) {
                                val status = apiResponse.subsonicResponse.status
                                if ("ok" == status) {
                                    isSuccess.setValue(true)
                                } else if ("failed" == status) {
                                    handleFailedResponse(apiResponse)
                                }
                            }
                        } else {
                            errorMessage.setValue("HTTP error: " + response.code())
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        errorMessage.setValue("Network error: " + t.message)
                    }
                })
        }
    }

    fun deleteRadio() {
        if (this.radioToEdit != null && radioToEdit!!.id != null) {
            errorMessage.setValue(null)
            isSuccess.setValue(false)

            radioRepository.deleteInternetRadioStation(radioToEdit!!.id)!!
                .enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            val apiResponse: ApiResponse = response.body()!!

                            val status = apiResponse.subsonicResponse.status

                            if ("ok" == status) {
                                isSuccess.setValue(true)
                            } else if ("failed" == status) {
                                handleFailedResponse(apiResponse)
                            }
                        } else {
                            errorMessage.setValue("HTTP error: " + response.code())
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        errorMessage.setValue("Network error: " + t.message)
                    }
                })
        }
    }

    private fun showError(message: String?) {
        Toast.makeText(getApplication<Application>(), message, Toast.LENGTH_LONG).show()
        errorMessage.setValue(message)
    }

    private fun handleFailedResponse(apiResponse: ApiResponse) {
        var errorMsg: String? = "Unknown server error"

        if (apiResponse.subsonicResponse.error != null) {
            errorMsg = apiResponse.subsonicResponse.error!!.message

            if ("Not implemented" == errorMsg) {
                errorMsg =
                    getApplication<Application>().getString((R.string.radio_dialog_not_supported_snackbar))
            }
        }

        Toast.makeText(getApplication<Application>(), errorMsg, Toast.LENGTH_LONG).show()

        errorMessage.setValue(errorMsg)
    }

    companion object {
        private const val TAG = "RadioEditorViewModel"
    }
}