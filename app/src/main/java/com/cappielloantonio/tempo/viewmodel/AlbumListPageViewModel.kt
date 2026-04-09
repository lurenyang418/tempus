package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.util.Constants
import java.util.Calendar
import java.util.Date
import kotlin.math.min

class AlbumListPageViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository: AlbumRepository

    @JvmField
    var title: String? = null
    @JvmField
    var artist: ArtistID3? = null

    private var albumList: MutableLiveData<MutableList<AlbumID3?>?>? = null

    @JvmField
    var maxNumber: Int = 500

    init {
        albumRepository = AlbumRepository()
    }

    fun getAlbumList(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        albumList = MutableLiveData<MutableList<AlbumID3?>?>(ArrayList<AlbumID3?>())

        when (title) {
            Constants.ALBUM_RECENTLY_PLAYED -> albumRepository.getAlbums(
                "recent",
                maxNumber,
                null,
                null
            ).observe(
                owner,
                Observer { albums: MutableList<AlbumID3?>? -> albumList!!.setValue(albums) })

            Constants.ALBUM_MOST_PLAYED -> albumRepository.getAlbums(
                "frequent",
                maxNumber,
                null,
                null
            ).observe(
                owner,
                Observer { albums: MutableList<AlbumID3?>? -> albumList!!.setValue(albums) })

            Constants.ALBUM_RECENTLY_ADDED -> albumRepository.getAlbums(
                "newest",
                maxNumber,
                null,
                null
            ).observe(
                owner,
                Observer { albums: MutableList<AlbumID3?>? -> albumList!!.setValue(albums) })

            Constants.ALBUM_STARRED -> albumList = albumRepository.getStarredAlbums(false, -1)
            Constants.ALBUM_NEW_RELEASES -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                albumRepository.getAlbums("byYear", maxNumber, currentYear, currentYear)
                    .observe(owner, Observer { albums: MutableList<AlbumID3?>? ->
                        albums!!.sortWith(
                            Comparator.comparing<AlbumID3?, Date?>(AlbumID3::created).reversed()
                        )
                        albumList!!.postValue(albums.subList(0, min(20, albums.size)))
                    })
            }
        }

        return albumList!!
    }
}
