package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class PodcastChannelBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository: PodcastRepository

    @JvmField
    var podcastChannel: PodcastChannel? = null

    init {
        podcastRepository = PodcastRepository()
    }

    fun deletePodcastChannel() {
        if (podcastChannel != null && podcastChannel!!.id != null) {
            podcastRepository.deletePodcastChannel(podcastChannel!!.id)!!
                .enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.code() == 501) {
                            Toast.makeText(
                                getApplication<Application>(),
                                "Podcasts are not supported by this server",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            val apiResponse: ApiResponse = response.body()!!

                            val status = apiResponse.subsonicResponse.status

                            if ("ok" == status) {
                                Toast.makeText(
                                    getApplication<Application>(),
                                    "Podcast channel deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //TODO refresh the UI after deleting
                                //podcastRepository.refreshPodcasts();
                            }
                        } else {
                            handleHttpError(response)
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        Toast.makeText(
                            getApplication<Application>(),
                            "Network error: " + t.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
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

        Toast.makeText(getApplication<Application>(), errorMsg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "PodcastChannelBottomSheetViewModel"
    }
}
