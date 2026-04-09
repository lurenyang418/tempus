package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PodcastRepository {
    fun getPodcastChannels(
        includeEpisodes: Boolean,
        channelId: String?
    ): MutableLiveData<MutableList<PodcastChannel?>?> {
        val livePodcastChannel =
            MutableLiveData<MutableList<PodcastChannel?>?>(ArrayList<PodcastChannel?>())

        getSubsonicClientInstance(false)
            .podcastClient!!
            .getPodcasts(includeEpisodes, channelId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.podcasts != null) {
                        livePodcastChannel.setValue(ArrayList(response.body()!!.subsonicResponse.podcasts!!.channels!!))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return livePodcastChannel
    }

    fun getNewestPodcastEpisodes(count: Int): MutableLiveData<MutableList<PodcastEpisode?>?> {
        val liveNewestPodcastEpisodes =
            MutableLiveData<MutableList<PodcastEpisode?>?>(ArrayList<PodcastEpisode?>())

        getSubsonicClientInstance(false)
            .podcastClient!!
            .getNewestPodcasts(count)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.newestPodcasts != null) {
                        liveNewestPodcastEpisodes.setValue(ArrayList(response.body()!!.subsonicResponse.newestPodcasts!!.episodes!!))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return liveNewestPodcastEpisodes
    }

    fun refreshPodcasts(): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .podcastClient!!
            .refreshPodcasts()
    }

    fun createPodcastChannel(url: String?): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .podcastClient!!
            .createPodcastChannel(url)
    }

    fun deletePodcastChannel(channelId: String?): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .podcastClient!!
            .deletePodcastChannel(channelId)
    }

    fun deletePodcastEpisode(episodeId: String?): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .podcastClient!!
            .deletePodcastEpisode(episodeId)
    }

    fun downloadPodcastEpisode(episodeId: String?): Call<ApiResponse?>? {
        return getSubsonicClientInstance(false)
            .podcastClient!!
            .downloadPodcastEpisode(episodeId)
    }

    companion object {
        private const val TAG = "PodcastRepository"
    }
}
