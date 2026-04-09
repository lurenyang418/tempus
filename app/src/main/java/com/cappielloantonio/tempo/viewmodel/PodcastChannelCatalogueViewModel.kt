package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.PodcastRepository
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel

class PodcastChannelCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository: PodcastRepository

    private val podcastChannels = MutableLiveData<MutableList<PodcastChannel?>?>(null)


    init {
        podcastRepository = PodcastRepository()
    }

    fun getPodcastChannels(owner: LifecycleOwner): LiveData<MutableList<PodcastChannel?>?> {
        if (podcastChannels.getValue() == null) {
            podcastRepository.getPodcastChannels(false, null).observe(
                owner,
                Observer { value: MutableList<PodcastChannel?>? -> podcastChannels.postValue(value) })
        }

        return podcastChannels
    }
}
