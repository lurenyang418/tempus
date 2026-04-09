package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode

class PodcastViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository: PodcastRepository

    private val newestPodcastEpisodes = MutableLiveData<MutableList<PodcastEpisode?>?>(null)
    private val podcastChannels = MutableLiveData<MutableList<PodcastChannel?>?>(null)

    init {
        podcastRepository = PodcastRepository()
    }

    fun getNewestPodcastEpisodes(owner: LifecycleOwner): LiveData<MutableList<PodcastEpisode?>?> {
        if (newestPodcastEpisodes.getValue() == null) {
            podcastRepository.getNewestPodcastEpisodes(20).observe(
                owner,
                Observer { value: MutableList<PodcastEpisode?>? ->
                    newestPodcastEpisodes.postValue(
                        value
                    )
                })
        }

        return newestPodcastEpisodes
    }

    fun getPodcastChannels(owner: LifecycleOwner): LiveData<MutableList<PodcastChannel?>?> {
        if (podcastChannels.getValue() == null) {
            podcastRepository.getPodcastChannels(false, null).observe(
                owner,
                Observer { value: MutableList<PodcastChannel?>? -> podcastChannels.postValue(value) })
        }

        return podcastChannels
    }

    fun refreshNewestPodcastEpisodes(owner: LifecycleOwner) {
        podcastRepository.getNewestPodcastEpisodes(20).observe(
            owner,
            Observer { value: MutableList<PodcastEpisode?>? -> newestPodcastEpisodes.postValue(value) })
    }

    fun refreshPodcastChannels(owner: LifecycleOwner) {
        podcastRepository.getPodcastChannels(false, null).observe(
            owner,
            Observer { value: MutableList<PodcastChannel?>? -> podcastChannels.postValue(value) })
    }
}
