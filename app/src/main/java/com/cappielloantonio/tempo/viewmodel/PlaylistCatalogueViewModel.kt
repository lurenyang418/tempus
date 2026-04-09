package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Playlist

class PlaylistCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistRepository: PlaylistRepository

    @JvmField
    var type: String? = null

    private val playlistList = MutableLiveData<MutableList<Playlist?>?>(null)

    init {
        playlistRepository = PlaylistRepository()
    }

    fun getPlaylistList(owner: LifecycleOwner): LiveData<MutableList<Playlist?>?> {
        if (playlistList.getValue() == null) {
            playlistRepository.getPlaylists(false, -1).observe(
                owner,
                Observer { value: MutableList<Playlist?>? -> playlistList.postValue(value) })
        }

        return playlistList
    }
}
