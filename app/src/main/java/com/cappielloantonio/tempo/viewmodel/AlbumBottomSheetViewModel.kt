package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences.isStarredAlbumsSyncEnabled
import java.util.Date
import java.util.stream.Collectors

class AlbumBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val favoriteRepository: FavoriteRepository
    private val sharingRepository: SharingRepository
    private var album: AlbumID3? = null
    private val instantMix = MutableLiveData<MutableList<Child?>?>(null)

    init {
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        favoriteRepository = FavoriteRepository()
        sharingRepository = SharingRepository()
    }

    fun getAlbum(): AlbumID3 {
        return album!!
    }

    fun setAlbum(album: AlbumID3) {
        this.album = album
    }

    val artist: LiveData<ArtistID3?>
        get() = artistRepository.getArtist(album!!.artistId)

    val albumTracks: MutableLiveData<MutableList<Child?>?>
        get() = albumRepository.getAlbumTracks(album!!.id)

    fun setFavorite(context: Context?) {
        if (album!!.starred != null) {
            if (NetworkUtil.isOffline) {
                removeFavoriteOffline()
            } else {
                removeFavoriteOnline()
            }
        } else {
            if (NetworkUtil.isOffline) {
                setFavoriteOffline()
            } else {
                setFavoriteOnline(context)
            }
        }
    }

    fun shareAlbum(): MutableLiveData<Share?> {
        return sharingRepository.createShare(album!!.id, album!!.name, null)
    }

    private fun removeFavoriteOffline() {
        favoriteRepository.starLater(null, album!!.id, null, false)
        album!!.starred = null
    }

    private fun removeFavoriteOnline() {
        favoriteRepository.unstar(null, album!!.id, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, album!!.id, null, false)
            }
        })

        album!!.starred = null
    }

    private fun setFavoriteOffline() {
        favoriteRepository.starLater(null, album!!.id, null, true)
        album!!.starred = Date()
    }

    private fun setFavoriteOnline(context: Context?) {
        favoriteRepository.star(null, album!!.id, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, album!!.id, null, true)
            }
        })

        album!!.starred = Date()
        if (isStarredAlbumsSyncEnabled()) {
            val albumRepository = AlbumRepository()
            val tracksLiveData = albumRepository.getAlbumTracks(album!!.id)

            tracksLiveData.observeForever(object : Observer<MutableList<Child?>?> {
                @OptIn(markerClass = [UnstableApi::class])
                override fun onChanged(songs: MutableList<Child?>?) {
                    if (songs != null && !songs.isEmpty()) {
                        val nonNullSongs = songs.filterNotNull()
                        if (nonNullSongs.isNotEmpty()) {
                            DownloadUtil.getDownloadTracker(context).download(
                                MappingUtil.mapDownloads(nonNullSongs.toMutableList()),
                                nonNullSongs.map { child -> Download(child) }.toMutableList()
                            )
                        }
                    }
                    tracksLiveData.removeObserver(this)
                }
            })
        }
    }

    fun getAlbumInstantMix(owner: LifecycleOwner, album: AlbumID3): LiveData<MutableList<Child?>?> {
        instantMix.setValue(mutableListOf<Child?>())

        albumRepository.getInstantMix(album, 30)!!
            .observe(owner, Observer { value: MutableList<Child?>? -> instantMix.postValue(value) })

        return instantMix
    }
}
