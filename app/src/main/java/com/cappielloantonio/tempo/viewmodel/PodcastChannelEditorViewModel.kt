package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class PodcastChannelEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository: PodcastRepository

    private val isSuccess = MutableLiveData<Boolean?>(false)
    private val errorMessage = MutableLiveData<String?>()

    init {
        podcastRepository = PodcastRepository()
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

    fun createChannel(url: String?) {
        errorMessage.setValue(null)
        isSuccess.setValue(false)

        podcastRepository.createPodcastChannel(url)!!
            .enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
                    if (response.code() == 501) {
                        showError(getApplication<Application>().getString(R.string.podcast_channel_not_supported_snackbar))
                        return
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        val apiResponse: ApiResponse = response.body()!!

                        val status = apiResponse.subsonicResponse.status
                        if ("ok" == status) {
                            isSuccess.setValue(true)
                        }
                    } else {
                        handleHttpError(response)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    showError("Network error: " + t.message)
                    Log.e(TAG, "Network error", t)
                }
            })
    }

    private fun handleHttpError(response: Response<ApiResponse?>) {
        var errorMsg = "HTTP error: " + response.code()
        if (response.errorBody() != null) {
            try {
                val serverMsg = response.errorBody()!!.string()
                if (!serverMsg.isEmpty()) {
                    errorMsg += " - " + serverMsg
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading error body", e)
            }
        }
        showError(errorMsg)
    }

    private fun showError(message: String?) {
        Toast.makeText(getApplication<Application>(), message, Toast.LENGTH_LONG).show()
        errorMessage.setValue(message)
        Log.e(TAG, "Error shown: " + message)
    }

    companion object {
        private const val TAG = "PodcastChannelEditorViewModel"
    }
}