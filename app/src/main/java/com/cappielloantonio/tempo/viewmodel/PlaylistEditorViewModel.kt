package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share
import com.google.common.collect.Lists

class PlaylistEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistRepository: PlaylistRepository
    private val sharingRepository: SharingRepository

    private var toAdd: java.util.ArrayList<Child?> = java.util.ArrayList<Child?>()
    private var toEdit: Playlist? = null

    private var songLiveList = MutableLiveData<MutableList<Child?>?>()

    init {
        playlistRepository = PlaylistRepository()
        sharingRepository = SharingRepository()
    }

    fun createPlaylist(name: String?) {
        playlistRepository.createPlaylist(
            null,
            name,
            java.util.ArrayList<String?>(Lists.transform(toAdd, Child::id))
        )
    }

    fun updatePlaylist(name: String?) {
        playlistRepository.updatePlaylist(toEdit!!.id, name, this.playlistSongIds)
    }

    fun deletePlaylist() {
        if (toEdit != null) playlistRepository.deletePlaylist(toEdit!!.id)
    }

    var songsToAdd: ArrayList<Child?>
        get() = toAdd
        set(songs) {
            toAdd = songs
        }

    var playlistToEdit: Playlist?
        get() = toEdit
        set(playlist) {
            this.toEdit = playlist

            if (playlist != null) {
                this.songLiveList = playlistRepository.getPlaylistSongs(toEdit!!.id)
            } else {
                this.songLiveList = MutableLiveData<MutableList<Child?>?>()
            }
        }

    val playlistSongLiveList: LiveData<MutableList<Child?>?>
        get() = songLiveList

    fun removeFromPlaylistSongLiveList(position: Int) {
        val songs = songLiveList.getValue()
        songs!!.removeAt(position)
        songLiveList.postValue(songs)
    }

    fun orderPlaylistSongLiveListAfterSwap(songs: MutableList<Child?>?) {
        songLiveList.postValue(songs)
    }

    private val playlistSongIds: ArrayList<String?>
        get() {
            val songs = songLiveList.getValue()
            val ids = java.util.ArrayList<String?>()

            if (songs != null && !songs.isEmpty()) {
                for (song in songs) {
                    ids.add(song?.id)
                }
            }

            return ids
        }

    fun sharePlaylist(): MutableLiveData<Share?> {
        return sharingRepository.createShare(toEdit!!.id, toEdit!!.name, null)
    }

    companion object {
        private const val TAG = "PlaylistEditorViewModel"
    }
}
