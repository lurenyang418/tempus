package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences.isStarredArtistsSyncEnabled
import java.util.Date
import java.util.stream.Collectors

class ArtistBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository: ArtistRepository
    private val favoriteRepository: FavoriteRepository
    private val instantMix = MutableLiveData<MutableList<Child?>?>(null)

    private var artist: ArtistID3? = null

    init {
        artistRepository = ArtistRepository()
        favoriteRepository = FavoriteRepository()
    }

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
                setFavoriteOffline(context)
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

    private fun setFavoriteOffline(context: Context?) {
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

        Log.d("ArtistSync", "Checking preference: " + isStarredArtistsSyncEnabled())

        if (isStarredArtistsSyncEnabled()) {
            Log.d("ArtistSync", "Starting artist sync for: " + artist!!.name)

            artistRepository.getArtistAllSongs(
                artist!!.id,
                object : ArtistRepository.ArtistSongsCallback {
                    @OptIn(markerClass = [UnstableApi::class])
                    override fun onSongsCollected(songs: MutableList<Child>?) {
                        Log.d(
                            "ArtistSync",
                            "Callback triggered with songs: " + (if (songs != null) songs.size else 0)
                        )
                        if (songs != null && !songs.isEmpty()) {
                            Log.d("ArtistSync", "Starting download of " + songs.size + " songs")
                            val nonNullSongs = songs.filterNotNull()
                            if (nonNullSongs.isNotEmpty()) {
                                DownloadUtil.getDownloadTracker(context).download(
                                    MappingUtil.mapDownloads(nonNullSongs.toMutableList()),
                                    nonNullSongs.stream().map<Download> { child -> Download(child) }
                                        .collect(Collectors.toList())
                                )
                            }
                            Log.d("ArtistSync", "Download started successfully")
                        } else {
                            Log.d("ArtistSync", "No songs to download")
                        }
                    }
                })
        } else {
            Log.d("ArtistSync", "Artist sync preference is disabled")
        }
    }

    fun getArtistInstantMix(
        owner: LifecycleOwner,
        artist: ArtistID3
    ): LiveData<MutableList<Child?>?> {
        instantMix.setValue(mutableListOf<Child?>())

        artistRepository.getInstantMix(artist, 30)!!
            .observe(owner, Observer { value: MutableList<Child?>? -> instantMix.postValue(value) })

        return instantMix
    }
}
