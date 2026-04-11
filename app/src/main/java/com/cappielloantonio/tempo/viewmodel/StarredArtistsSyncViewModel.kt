package com.cappielloantonio.tempo.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import java.util.concurrent.atomic.AtomicInteger

class StarredArtistsSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository: ArtistRepository

    private val starredArtists = MutableLiveData<MutableList<ArtistID3?>?>(null)
    private val starredArtistSongs = MutableLiveData<MutableList<Child?>?>(null)

    init {
        artistRepository = ArtistRepository()
    }

    fun getStarredArtists(owner: LifecycleOwner): LiveData<MutableList<ArtistID3?>?> {
        artistRepository.getStarredArtists(false, -1).observe(
            owner,
            Observer { value: MutableList<ArtistID3?>? -> starredArtists.postValue(value) })
        return starredArtists
    }

    val allStarredArtistSongs: LiveData<MutableList<Child?>?>
        get() {
            artistRepository.getStarredArtists(false, -1).observeForever(
                object : Observer<MutableList<ArtistID3?>?> {
                override fun onChanged(value: MutableList<ArtistID3?>?) {
                    if (!value.isNullOrEmpty()) {
                        collectAllArtistSongs(
                            value.filterNotNull(),
                            object : StarredArtistsSyncViewModel.ArtistSongsCallback {
                                override fun onSongsCollected(songs: MutableList<Child?>?) {
                                    starredArtistSongs.postValue(songs)
                                }
                            })
                    } else {
                        starredArtistSongs.postValue(ArrayList<Child?>())
                    }
                    artistRepository.getStarredArtists(false, -1).removeObserver(this)
                }
            })

            return starredArtistSongs
        }

    fun getStarredArtistSongs(activity: Activity?): LiveData<MutableList<Child?>?> {
        artistRepository.getStarredArtists(false, -1)
            .observe((activity as LifecycleOwner?)!!, object : Observer<MutableList<ArtistID3?>?> {
                override fun onChanged(value: MutableList<ArtistID3?>?) {
                    if (!value.isNullOrEmpty()) {
                        collectAllArtistSongs(
                            value.filterNotNull(),
                            object : StarredArtistsSyncViewModel.ArtistSongsCallback {
                                override fun onSongsCollected(songs: MutableList<Child?>?) {
                                    starredArtistSongs.postValue(songs)
                                }
                            })
                    } else {
                        starredArtistSongs.postValue(ArrayList<Child?>())
                    }
                }
            })
        return starredArtistSongs
    }

    private fun collectAllArtistSongs(
        artists: List<ArtistID3>,
        callback: ArtistSongsCallback
    ) {
        if (artists.isEmpty()) {
            callback.onSongsCollected(ArrayList<Child?>())
            return
        }

        val allSongs: MutableList<Child?> = ArrayList<Child?>()
        val remainingArtists = AtomicInteger(artists.size)

        for (artist in artists) {
            artistRepository.getArtistAllSongs(
                artist.id,
                object : ArtistRepository.ArtistSongsCallback {
                    override fun onSongsCollected(songs: MutableList<Child>?) {
                        if (songs != null) {
                            allSongs.addAll(songs)
                        }

                        val remaining = remainingArtists.decrementAndGet()
                        if (remaining == 0) {
                            callback.onSongsCollected(allSongs)
                        }
                    }
                })
        }
    }

    private interface ArtistSongsCallback {
        fun onSongsCollected(songs: MutableList<Child?>?)
    }
}