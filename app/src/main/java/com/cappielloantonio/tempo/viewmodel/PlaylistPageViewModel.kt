package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist

class PlaylistPageViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistRepository: PlaylistRepository

    private var playlist: Playlist? = null
    private val isOffline = false

    private val songLiveList = MutableLiveData<MutableList<Child?>?>()

    init {
        playlistRepository = PlaylistRepository()
        PlaylistRepository.Companion.playlistUpdateTrigger.observeForever(Observer { needsRefresh: Boolean? ->
            if (needsRefresh != null && needsRefresh && playlist != null) {
                refreshSongs()
            }
        })
    }

    val playlistSongLiveList: LiveData<MutableList<Child?>?>
        get() {
            if (songLiveList.getValue() == null && playlist != null) {
                refreshSongs()
            }
            return songLiveList
        }

    private fun refreshSongs() {
        if (playlist == null) return
        val remoteData: LiveData<MutableList<Child?>?> =
            playlistRepository.getPlaylistSongs(playlist!!.id)
        remoteData.observeForever(object : Observer<MutableList<Child?>?> {
            override fun onChanged(value: MutableList<Child?>?) {
                songLiveList.postValue(value)
                remoteData.removeObserver(this)
            }
        })
    }

    fun getPlaylist(): Playlist? {
        return playlist
    }

    fun setPlaylist(playlist: Playlist) {
        if (this.playlist == null || this.playlist!!.id != playlist.id) {
            this.playlist = playlist
            this.songLiveList.setValue(null) // Clear old data immediately
        }
    }

    fun isPinned(owner: LifecycleOwner): LiveData<Boolean?> {
        val isPinnedLive = MutableLiveData<Boolean?>()

        playlistRepository.pinnedPlaylists.observe(
            owner,
            Observer { playlists: MutableList<Playlist> ->
                isPinnedLive.postValue(
                    playlists.any { obj -> obj.id == playlist?.id })
            })

        return isPinnedLive
    }

    fun setPinned(isNowPinned: Boolean) {
        playlist?.let {
            if (isNowPinned) {
                playlistRepository.insert(it)
            } else {
                playlistRepository.delete(it)
            }
        }
    }
}
