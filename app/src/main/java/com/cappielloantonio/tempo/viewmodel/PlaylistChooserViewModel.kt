package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.app.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.util.Preferences.allowPlaylistDuplicates
import com.google.common.collect.Lists

class PlaylistChooserViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistRepository: PlaylistRepository
    private val playlists = MutableLiveData<MutableList<Playlist?>?>(null)
    private val playlistIsPublic = MutableLiveData<Boolean?>(false)

    var isPlaylistPublic: Boolean?
        get() = playlistIsPublic.getValue()
        set(isPublic) {
            playlistIsPublic.setValue(isPublic)
        }

    fun setIsPlaylistPublic(isPublic: Boolean?) {
        playlistIsPublic.setValue(isPublic)
    }

    var songsToAdd: ArrayList<Child?> = ArrayList<Child?>()

    init {
        playlistRepository = PlaylistRepository()
    }

    fun getPlaylistList(owner: LifecycleOwner): LiveData<MutableList<Playlist?>?> {
        playlistRepository.getPlaylists(false, -1).observe(
            owner,
            Observer { value: MutableList<Playlist?>? -> playlists.postValue(value) })
        return playlists
    }

    fun addSongsToPlaylist(owner: LifecycleOwner, dialog: Dialog, playlistId: String?) {
        val songIds = Lists.transform<Child?, String?>(
            this.songsToAdd, Child::id
        )
        if (allowPlaylistDuplicates()) {
            playlistRepository.addSongToPlaylist(
                playlistId, ArrayList<String?>(songIds),
                this.isPlaylistPublic!!
            )
            dialog.dismiss()
        } else {
            playlistRepository.getPlaylistSongs(playlistId)
                .observe(owner, Observer { playlistSongs: MutableList<Child?>? ->
                    if (playlistSongs != null) {
                        val playlistSongIds =
                            Lists.transform<Child?, String?>(playlistSongs, Child::id)
                        songIds.removeAll(playlistSongIds)
                    }
                    playlistRepository.addSongToPlaylist(
                        playlistId, ArrayList<String?>(songIds),
                        this.isPlaylistPublic!!
                    )
                    dialog.dismiss()
                })
        }
    }
}
