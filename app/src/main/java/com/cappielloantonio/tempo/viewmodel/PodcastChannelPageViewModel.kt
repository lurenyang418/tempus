package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode

class PodcastChannelPageViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository: PodcastRepository

    private var podcastChannel: PodcastChannel? = null

    init {
        podcastRepository = PodcastRepository()
    }

    val podcastChannelEpisodes: LiveData<MutableList<PodcastChannel?>?>
        get() = podcastRepository.getPodcastChannels(true, podcastChannel!!.id)

    fun getPodcastChannel(): PodcastChannel {
        return podcastChannel!!
    }

    fun setPodcastChannel(podcastChannel: PodcastChannel) {
        this.podcastChannel = podcastChannel
    }

    fun requestPodcastEpisodeDownload(podcastEpisode: PodcastEpisode) {
        podcastRepository.downloadPodcastEpisode(podcastEpisode.id)
    }
}
