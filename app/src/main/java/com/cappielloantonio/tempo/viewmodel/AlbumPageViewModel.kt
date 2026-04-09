package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.AlbumInfo
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.NetworkUtil
import java.util.Date

class AlbumPageViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val favoriteRepository: FavoriteRepository
    private var albumId: String? = null
    private var artistId: String? = null
    @JvmField
    val album: MutableLiveData<AlbumID3?> = MutableLiveData<AlbumID3?>(null)

    init {
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        favoriteRepository = FavoriteRepository()
    }

    val albumSongLiveList: LiveData<MutableList<Child?>?>
        get() = albumRepository.getAlbumTracks(albumId)

    fun setAlbum(owner: LifecycleOwner, album: AlbumID3) {
        this.albumId = album.id
        this.album.postValue(album)
        this.artistId = album.artistId

        albumRepository.getAlbum(album.id).observe(owner, Observer { albums: AlbumID3? ->
            if (albums != null) this.album.setValue(albums)
        })
    }

    fun setFavorite() {
        val currentAlbum = album.getValue()
        if (currentAlbum == null) return

        if (currentAlbum.starred != null) {
            if (NetworkUtil.isOffline) {
                removeFavoriteOffline(currentAlbum)
            } else {
                removeFavoriteOnline(currentAlbum)
            }
        } else {
            if (NetworkUtil.isOffline) {
                setFavoriteOffline(currentAlbum)
            } else {
                setFavoriteOnline(currentAlbum)
            }
        }
    }

    private fun removeFavoriteOffline(album: AlbumID3) {
        favoriteRepository.starLater(null, album.id, null, false)
        album.starred = null
        this.album.postValue(album)
    }

    private fun removeFavoriteOnline(album: AlbumID3) {
        favoriteRepository.unstar(null, album.id, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, album.id, null, false)
            }
        })

        album.starred = null
        this.album.postValue(album)
    }

    private fun setFavoriteOffline(album: AlbumID3) {
        favoriteRepository.starLater(null, album.id, null, true)
        album.starred = Date()
        this.album.postValue(album)
    }

    private fun setFavoriteOnline(album: AlbumID3) {
        favoriteRepository.star(null, album.id, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, album.id, null, true)
            }
        })

        album.starred = Date()
        this.album.postValue(album)
    }

    val artist: LiveData<ArtistID3?>
        get() = artistRepository.getArtistInfo(artistId)

    val albumInfo: LiveData<AlbumInfo?>
        get() = albumRepository.getAlbumInfo(albumId)
}
