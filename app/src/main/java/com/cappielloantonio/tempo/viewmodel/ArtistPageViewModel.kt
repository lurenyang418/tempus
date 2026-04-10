package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.ArtistInfo2
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences.isStarredArtistsSyncEnabled
import java.util.Date
import java.util.stream.Collectors

class ArtistPageViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val favoriteRepository: FavoriteRepository

    private var artist: ArtistID3? = null

    init {
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        favoriteRepository = FavoriteRepository()
    }

    val albumList: LiveData<MutableList<AlbumID3?>?>
        get() = albumRepository.getArtistAlbums(artist!!.id)

    fun getArtistInfo(id: String?): LiveData<ArtistInfo2?> {
        return artistRepository.getArtistFullInfo(id)
    }

    val artistTopSongList: LiveData<MutableList<Child?>?>
        get() = artistRepository.getTopSongs(artist!!.name, 20)

    val artistShuffleList: LiveData<MutableList<Child?>?>
        get() = artistRepository.getRandomSong(artist!!, 50)

    val artistInstantMix: LiveData<MutableList<Child?>?>?
        get() = artistRepository.getInstantMix(artist!!, 30)

    fun getArtist(): ArtistID3 {
        return artist!!
    }

    fun setArtist(artist: ArtistID3) {
        this.artist = artist
    }

    fun setFavorite(context: Context?) {
        if (artist!!.starred != null) {
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

    private fun removeFavoriteOffline() {
        favoriteRepository.starLater(null, null, artist!!.id, false)
        artist!!.starred = null
    }

    private fun removeFavoriteOnline() {
        favoriteRepository.unstar(null, null, artist!!.id, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, null, artist!!.id, false)
            }
        })

        artist!!.starred = null
    }

    private fun setFavoriteOffline() {
        favoriteRepository.starLater(null, null, artist!!.id, true)
        artist!!.starred = Date()
    }

    private fun setFavoriteOnline(context: Context?) {
        favoriteRepository.star(null, null, artist!!.id, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, null, artist!!.id, true)
            }
        })

        artist!!.starred = Date()

        if (isStarredArtistsSyncEnabled()) {
            artistRepository.getArtistAllSongs(
                artist!!.id,
                object : ArtistRepository.ArtistSongsCallback {
                    @OptIn(markerClass = [UnstableApi::class])
                    override fun onSongsCollected(songs: MutableList<Child>?) {
                        if (songs != null && !songs.isEmpty()) {
                            val nonNullSongs = songs.filterNotNull()
                            if (nonNullSongs.isNotEmpty()) {
                                val safeContext = context ?: return
                                DownloadUtil.getDownloadTracker(safeContext).download(
                                    MappingUtil.mapDownloads(nonNullSongs.toMutableList()),
                                    nonNullSongs.map { child -> Download(child) }.toMutableList()
                                )
                            }
                        }
                    }
                })
        } else {
            Log.d("ArtistSync", "Artist sync preference is disabled")
        }
    }
}
