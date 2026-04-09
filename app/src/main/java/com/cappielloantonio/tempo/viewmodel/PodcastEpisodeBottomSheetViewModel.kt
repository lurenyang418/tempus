package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode

class PodcastEpisodeBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository: PodcastRepository

    @JvmField
    var podcastEpisode: PodcastEpisode? = null

    init {
        podcastRepository = PodcastRepository()
    }

    fun deletePodcastEpisode() {
        if (podcastEpisode != null) podcastRepository.deletePodcastEpisode(podcastEpisode!!.id)
    }
}
