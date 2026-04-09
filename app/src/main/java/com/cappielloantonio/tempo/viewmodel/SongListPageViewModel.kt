package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.util.Constants

class SongListPageViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val artistRepository: ArtistRepository

    @JvmField
    var title: String? = null
    @JvmField
    var toolbarTitle: String? = null
    @JvmField
    var genre: Genre? = null
    @JvmField
    var artist: ArtistID3? = null
    @JvmField
    var album: AlbumID3? = null

    private var songList: MutableLiveData<MutableList<Child?>?>? = null

    @JvmField
    var filters: ArrayList<String?> = ArrayList<String?>()
    @JvmField
    var filterNames: ArrayList<String?> = ArrayList<String?>()

    @JvmField
    var year: Int = 0
    @JvmField
    var maxNumberByYear: Int = 500
    @JvmField
    var maxNumberByGenre: Int = 500

    init {
        songRepository = SongRepository()
        artistRepository = ArtistRepository()
    }

    fun getSongList(): LiveData<MutableList<Child?>?> {
        songList = MutableLiveData<MutableList<Child?>?>(ArrayList<Child?>())

        when (title) {
            Constants.MEDIA_BY_GENRE -> songList =
                songRepository.getRandomSampleWithGenre(maxNumberByGenre, 0, 3000, genre!!.genre)

            Constants.MEDIA_BY_ARTIST -> songList = artistRepository.getTopSongs(artist!!.name, 50)
            Constants.MEDIA_BY_GENRES -> songList = songRepository.getSongsByGenres(filters)
            Constants.MEDIA_BY_YEAR -> songList =
                songRepository.getRandomSample(maxNumberByYear, year, year + 10)

            Constants.MEDIA_STARRED -> songList = songRepository.getStarredSongs(false, -1)
        }

        return songList!!
    }

    fun getSongsByPage(owner: LifecycleOwner) {
        when (title) {
            Constants.MEDIA_BY_GENRE -> {
                val songCount =
                    if (songList!!.getValue() != null) songList!!.getValue()!!.size else 0

                if (songCount > 0 && songCount % maxNumberByGenre != 0) return

                val page = songCount / maxNumberByGenre
                songRepository.getSongsByGenre(genre!!.genre, page)
                    .observe(owner, Observer { children: MutableList<Child?>? ->
                        if (children != null && !children.isEmpty()) {
                            val currentMedia = songList!!.getValue()
                            currentMedia!!.addAll(children)
                            songList!!.setValue(currentMedia)
                        }
                    })
            }

            Constants.MEDIA_BY_ARTIST, Constants.MEDIA_BY_GENRES, Constants.MEDIA_BY_YEAR, Constants.MEDIA_STARRED -> {}
        }
    }

    val filtersTitle: String?
        get() = TextUtils.join(", ", filterNames)
}
