package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.util.Constants
import java.util.TreeSet
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

class ArtistListPageViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository: ArtistRepository
    private val downloadRepository: DownloadRepository

    @JvmField
    var title: String? = null

    private var artistList: MutableLiveData<MutableList<ArtistID3?>?>? = null

    init {
        artistRepository = ArtistRepository()
        downloadRepository = DownloadRepository()
    }

    fun getArtistList(owner: LifecycleOwner): LiveData<MutableList<ArtistID3?>?>? {
        artistList = MutableLiveData<MutableList<ArtistID3?>?>(java.util.ArrayList<ArtistID3?>())

        when (title) {
            Constants.ARTIST_STARRED -> artistList = artistRepository.getStarredArtists(false, -1)
            Constants.ARTIST_DOWNLOADED -> downloadRepository.liveDownload!!.observe(
                owner,
                Observer { downloads: MutableList<Download?>? ->
                    val unique = downloads!!
                        .distinctBy { it?.artist }
                        .toMutableList()
                })
        }

        return artistList
    }
}
