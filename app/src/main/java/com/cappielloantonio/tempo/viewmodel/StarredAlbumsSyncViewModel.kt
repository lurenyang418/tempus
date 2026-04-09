package com.cappielloantonio.tempo.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.Child
import java.util.concurrent.CountDownLatch

class StarredAlbumsSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository: AlbumRepository

    private val starredAlbums = MutableLiveData<MutableList<AlbumID3?>?>(null)
    private val starredAlbumSongs = MutableLiveData<MutableList<Child?>?>(null)

    init {
        albumRepository = AlbumRepository()
    }

    fun getStarredAlbums(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        albumRepository.getStarredAlbums(false, -1).observe(
            owner,
            Observer { value: MutableList<AlbumID3?>? -> starredAlbums.postValue(value) })
        return starredAlbums
    }

    @Suppress("UNCHECKED_CAST")
    val allStarredAlbumSongs: LiveData<MutableList<Child?>?>
        get() {
            albumRepository.getStarredAlbums(false, -1).observeForever(object : Observer<MutableList<AlbumID3?>?> {
                override fun onChanged(albums: MutableList<AlbumID3?>?) {
                    if (!albums.isNullOrEmpty()) {
                        collectAllAlbumSongs(
                            albums.filterNotNull(),
                            object : AlbumSongsCallback {
                                override fun onSongsCollected(value: MutableList<Child?>?) {
                                    starredAlbumSongs.postValue(value)
                                }
                            })
                    } else {
                        starredAlbumSongs.postValue(ArrayList<Child?>())
                    }
                    albumRepository.getStarredAlbums(false, -1).removeObserver(this)
                }
            })

            return starredAlbumSongs
        }

    @Suppress("UNCHECKED_CAST")
    fun getStarredAlbumSongs(activity: Activity?): LiveData<MutableList<Child?>?> {
        albumRepository.getStarredAlbums(false, -1)
            .observe((activity as LifecycleOwner?)!!, object : Observer<MutableList<AlbumID3?>?> {
                override fun onChanged(albums: MutableList<AlbumID3?>?) {
                    if (!albums.isNullOrEmpty()) {
                        collectAllAlbumSongs(
                            albums.filterNotNull(),
                            object : AlbumSongsCallback {
                                override fun onSongsCollected(value: MutableList<Child?>?) {
                                    starredAlbumSongs.postValue(value)
                                }
                            })
                    } else {
                        starredAlbumSongs.postValue(ArrayList<Child?>())
                    }
                }
            })
        return starredAlbumSongs
    }

    private fun collectAllAlbumSongs(albums: List<AlbumID3>, callback: AlbumSongsCallback) {
        val allSongs: MutableList<Child?> = ArrayList<Child?>()
        val latch = CountDownLatch(albums.size)

        for (album in albums) {
            val albumTracks: LiveData<MutableList<Child?>?> =
                albumRepository.getAlbumTracks(album.id)
            albumTracks.observeForever(object : Observer<MutableList<Child?>?> {
                override fun onChanged(songs: MutableList<Child?>?) {
                    if (songs != null) {
                        allSongs.addAll(songs)
                    }
                    latch.countDown()

                    if (latch.getCount() == 0L) {
                        callback.onSongsCollected(allSongs)
                        albumTracks.removeObserver(this)
                    }
                }
            })
        }
    }

    private interface AlbumSongsCallback {
        fun onSongsCollected(songs: MutableList<Child?>?)
    }
}