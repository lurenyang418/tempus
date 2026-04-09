package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.Child

class StarredSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository

    private val starredTracks = MutableLiveData<MutableList<Child?>?>(null)

    init {
        songRepository = SongRepository()
    }

    fun getStarredTracks(owner: LifecycleOwner): LiveData<MutableList<Child?>?> {
        songRepository.getStarredSongs(false, -1).observe(
            owner,
            Observer { value: MutableList<Child?>? -> starredTracks.postValue(value) })
        return starredTracks
    }
}
