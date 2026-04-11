package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.model.Favorite
import com.cappielloantonio.tempo.model.HomeSector
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.ChronologyRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Constants.SeedType
import com.cappielloantonio.tempo.util.Preferences.getHomeSectorList
import com.cappielloantonio.tempo.util.Preferences.getHomeSortPlaylists
import com.cappielloantonio.tempo.util.Preferences.getServerId
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.Calendar
import java.util.Collections
import java.util.Date
import kotlin.math.min

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val chronologyRepository: ChronologyRepository
    private val favoriteRepository: FavoriteRepository
    private val playlistRepository: PlaylistRepository
    private val sharingRepository: SharingRepository

    private val albumsSyncViewModel: StarredAlbumsSyncViewModel
    private val artistSyncViewModel: StarredArtistsSyncViewModel

    private val dicoverSongSample = MutableLiveData<MutableList<Child?>?>(null)
    private val newReleasedAlbum = MutableLiveData<MutableList<AlbumID3?>?>(null)
    private val starredTracksSample = MutableLiveData<MutableList<Child?>?>(null)
    private val starredArtistsSample = MutableLiveData<MutableList<ArtistID3?>?>(null)
    private val bestOfArtists = MutableLiveData<MutableList<ArtistID3?>?>(null)
    private val starredTracks = MutableLiveData<MutableList<Child?>?>(null)
    private val starredAlbums = MutableLiveData<MutableList<AlbumID3?>?>(null)
    private val starredArtists = MutableLiveData<MutableList<ArtistID3?>?>(null)
    private val mostPlayedAlbumSample = MutableLiveData<MutableList<AlbumID3?>?>(null)
    private val recentlyPlayedAlbumSample = MutableLiveData<MutableList<AlbumID3?>?>(null)
    private val years = MutableLiveData<MutableList<Int?>?>(null)
    private val recentlyAddedAlbumSample = MutableLiveData<MutableList<AlbumID3?>?>(null)

    private val thisGridTopSong = MutableLiveData<MutableList<Chronology>?>(null)
    private val mediaInstantMix = MutableLiveData<MutableList<Child?>?>(null)
    private val artistInstantMix = MutableLiveData<MutableList<Child?>?>(null)
    private val artistBestOf = MutableLiveData<MutableList<Child?>?>(null)
    private val pinnedPlaylists = MutableLiveData<MutableList<Playlist?>?>(null)
    private val shares = MutableLiveData<MutableList<Share?>?>(null)

    var homeSectorList: MutableList<HomeSector?>? = null
        private set

    init {
        setHomeSectorList()

        songRepository = SongRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        chronologyRepository = ChronologyRepository()
        favoriteRepository = FavoriteRepository()
        playlistRepository = PlaylistRepository()
        sharingRepository = SharingRepository()

        albumsSyncViewModel = StarredAlbumsSyncViewModel(application)
        artistSyncViewModel = StarredArtistsSyncViewModel(application)

        setOfflineFavorite()
    }

    fun getDiscoverSongSample(owner: LifecycleOwner): LiveData<MutableList<Child?>?> {
        if (dicoverSongSample.getValue() == null) {
            songRepository.getRandomSample(10, null, null).observe(
                owner,
                Observer { value: MutableList<Child?>? -> dicoverSongSample.postValue(value) })
        }

        return dicoverSongSample
    }

    val randomShuffleSample: LiveData<MutableList<Child?>?>
        get() = songRepository.getRandomSample(100, null, null)

    fun getChronologySample(owner: LifecycleOwner): LiveData<MutableList<Chronology>?> {
        val cal = Calendar.getInstance()
        val server = getServerId()

        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val start = cal.getTimeInMillis()

        cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
        val end = cal.getTimeInMillis()

        chronologyRepository.getChronology(server, start, end).observe(
            owner,
            Observer { value: MutableList<Chronology> -> thisGridTopSong.postValue(value) })
        return thisGridTopSong
    }

    fun getRecentlyReleasedAlbums(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        if (newReleasedAlbum.getValue() == null) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            albumRepository.getAlbums("byYear", 500, currentYear, currentYear)
                .observe(owner, Observer { albums: MutableList<AlbumID3?>? ->
                    if (albums != null) {
                        albums.sortWith(
                            Comparator.comparing<AlbumID3?, Date?>(AlbumID3::created).reversed()
                        )
                        newReleasedAlbum.postValue(albums.subList(0, min(20, albums.size)))
                    }
                })
        }

        return newReleasedAlbum
    }

    fun getStarredTracksSample(owner: LifecycleOwner): LiveData<MutableList<Child?>?> {
        if (starredTracksSample.getValue() == null) {
            songRepository.getStarredSongs(true, 10).observe(
                owner,
                Observer { value: MutableList<Child?>? -> starredTracksSample.postValue(value) })
        }

        return starredTracksSample
    }

    fun getStarredArtistsSample(owner: LifecycleOwner): LiveData<MutableList<ArtistID3?>?> {
        if (starredArtistsSample.getValue() == null) {
            artistRepository.getStarredArtists(true, 10).observe(
                owner,
                Observer { value: MutableList<ArtistID3?>? -> starredArtistsSample.postValue(value) })
        }

        return starredArtistsSample
    }

    fun getBestOfArtists(owner: LifecycleOwner): LiveData<MutableList<ArtistID3?>?> {
        if (bestOfArtists.getValue() == null) {
            artistRepository.getStarredArtists(true, 20).observe(
                owner,
                Observer { value: MutableList<ArtistID3?>? -> bestOfArtists.postValue(value) })
        }

        return bestOfArtists
    }

    fun getStarredTracks(owner: LifecycleOwner): LiveData<MutableList<Child?>?> {
        if (starredTracks.getValue() == null) {
            songRepository.getStarredSongs(true, 20).observe(
                owner,
                Observer { value: MutableList<Child?>? -> starredTracks.postValue(value) })
        }

        return starredTracks
    }

    fun getStarredAlbums(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        if (starredAlbums.getValue() == null) {
            albumRepository.getStarredAlbums(true, 20).observe(
                owner,
                Observer { value: MutableList<AlbumID3?>? -> starredAlbums.postValue(value) })
        }

        return starredAlbums
    }

    val allStarredAlbumSongs: LiveData<MutableList<Child?>?>?
        get() = albumsSyncViewModel.allStarredAlbumSongs

    val allStarredArtistSongs: LiveData<MutableList<Child?>?>?
        get() = artistSyncViewModel.allStarredArtistSongs

    fun getStarredArtists(owner: LifecycleOwner): LiveData<MutableList<ArtistID3?>?> {
        if (starredArtists.getValue() == null) {
            artistRepository.getStarredArtists(true, 20).observe(
                owner,
                Observer { value: MutableList<ArtistID3?>? -> starredArtists.postValue(value) })
        }

        return starredArtists
    }

    fun getYearList(owner: LifecycleOwner): LiveData<MutableList<Int?>?> {
        if (years.getValue() == null) {
            albumRepository.decades.observe(
                owner,
                Observer { value: MutableList<Int?>? -> years.postValue(value) })
        }

        return years
    }

    fun getMostPlayedAlbums(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        if (mostPlayedAlbumSample.getValue() == null) {
            albumRepository.getAlbums("frequent", 20, null, null).observe(
                owner,
                Observer { value: MutableList<AlbumID3?>? -> mostPlayedAlbumSample.postValue(value) })
        }

        return mostPlayedAlbumSample
    }

    fun getMostRecentlyAddedAlbums(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        if (recentlyAddedAlbumSample.getValue() == null) {
            albumRepository.getAlbums("newest", 20, null, null).observe(
                owner,
                Observer { value: MutableList<AlbumID3?>? ->
                    recentlyAddedAlbumSample.postValue(
                        value
                    )
                })
        }

        return recentlyAddedAlbumSample
    }

    fun getRecentlyPlayedAlbumList(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        if (recentlyPlayedAlbumSample.getValue() == null) {
            albumRepository.getAlbums("recent", 20, null, null).observe(
                owner,
                Observer { value: MutableList<AlbumID3?>? ->
                    recentlyPlayedAlbumSample.postValue(
                        value
                    )
                })
        }

        return recentlyPlayedAlbumSample
    }

    fun getMediaInstantMix(owner: LifecycleOwner, media: Child): LiveData<MutableList<Child?>?> {
        mediaInstantMix.setValue(mutableListOf<Child?>())

        songRepository.getInstantMix(media.id, SeedType.TRACK, 20).observe(
            owner,
            Observer { value: MutableList<Child?>? -> mediaInstantMix.postValue(value) })

        return mediaInstantMix
    }

    fun getArtistInstantMix(
        owner: LifecycleOwner,
        artist: ArtistID3
    ): LiveData<MutableList<Child?>?> {
        artistInstantMix.setValue(mutableListOf<Child?>())

        artistRepository.getTopSongs(artist.name, 10).observe(
            owner,
            Observer { value: MutableList<Child?>? -> artistInstantMix.postValue(value) })

        return artistInstantMix
    }

    fun getArtistBestOf(owner: LifecycleOwner, artist: ArtistID3): LiveData<MutableList<Child?>?> {
        artistBestOf.setValue(mutableListOf<Child?>())

        artistRepository.getTopSongs(artist.name, 10).observe(
            owner,
            Observer { value: MutableList<Child?>? -> artistBestOf.postValue(value) })

        return artistBestOf
    }

    fun getPinnedPlaylists(owner: LifecycleOwner): LiveData<MutableList<Playlist?>?> {
        pinnedPlaylists.setValue(mutableListOf<Playlist?>())

        playlistRepository.getPlaylists(false, -1)
            .observe(owner, Observer { remotes: MutableList<Playlist?>? ->
                if (remotes != null && !remotes.isEmpty()) {
                    val playlists: MutableList<Playlist?> = java.util.ArrayList<Playlist?>(remotes)
                    val result = getHomeSortPlaylists()
                    if (getHomeSortPlaylists() == Constants.PLAYLIST_ORDER_BY_RANDOM) {
                        Collections.shuffle(playlists)
                    } else {
                        playlists.sortWith(Comparator.comparing<Playlist?, String>({ p -> p?.name ?: "" }))
                    }
                    val subsetPlaylists = if (playlists.size > 5)
                        playlists.subList(0, 5)
                    else
                        playlists

                    pinnedPlaylists.setValue(subsetPlaylists)
                }
            })

        return pinnedPlaylists
    }

    fun getShares(owner: LifecycleOwner): LiveData<MutableList<Share?>?> {
        if (shares.getValue() == null) {
            sharingRepository.shares.observe(
                owner,
                Observer { value: MutableList<Share?>? -> shares.postValue(value) })
        }

        return shares
    }

    val allStarredTracks: LiveData<MutableList<Child?>?>
        get() = songRepository.getStarredSongs(false, -1)

    fun changeChronologyPeriod(owner: LifecycleOwner, period: Int) {
        val cal = Calendar.getInstance()
        val server = getServerId()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)

        var start: Long = 0
        var end: Long = 0

        if (period == 0) {
            start = cal.getTimeInMillis()
            cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
            end = cal.getTimeInMillis()
        } else if (period == 1) {
            start = cal.getTimeInMillis()
            cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 4)
            end = cal.getTimeInMillis()
        } else if (period == 2) {
            start = cal.getTimeInMillis()
            cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 52)
            end = cal.getTimeInMillis()
        }

        chronologyRepository.getChronology(server, start, end).observe(
            owner,
            Observer { value: MutableList<Chronology> -> thisGridTopSong.postValue(value) })
    }

    fun refreshDiscoverySongSample(owner: LifecycleOwner) {
        songRepository.getRandomSample(10, null, null).observe(
            owner,
            Observer { value: MutableList<Child?>? -> dicoverSongSample.postValue(value) })
    }

    fun refreshSimilarSongSample(owner: LifecycleOwner) {
        songRepository.getStarredSongs(true, 10).observe(
            owner,
            Observer { value: MutableList<Child?>? -> starredTracksSample.postValue(value) })
    }

    fun refreshRadioArtistSample(owner: LifecycleOwner) {
        artistRepository.getStarredArtists(true, 10).observe(
            owner,
            Observer { value: MutableList<ArtistID3?>? -> starredArtistsSample.postValue(value) })
    }

    fun refreshBestOfArtist(owner: LifecycleOwner) {
        artistRepository.getStarredArtists(true, 20).observe(
            owner,
            Observer { value: MutableList<ArtistID3?>? -> bestOfArtists.postValue(value) })
    }

    fun refreshStarredTracks(owner: LifecycleOwner) {
        songRepository.getStarredSongs(true, 20).observe(
            owner,
            Observer { value: MutableList<Child?>? -> starredTracks.postValue(value) })
    }

    fun refreshStarredAlbums(owner: LifecycleOwner) {
        albumRepository.getStarredAlbums(true, 20).observe(
            owner,
            Observer { value: MutableList<AlbumID3?>? -> starredAlbums.postValue(value) })
    }

    fun refreshStarredArtists(owner: LifecycleOwner) {
        artistRepository.getStarredArtists(true, 20).observe(
            owner,
            Observer { value: MutableList<ArtistID3?>? -> starredArtists.postValue(value) })
    }

    fun refreshMostPlayedAlbums(owner: LifecycleOwner) {
        albumRepository.getAlbums("frequent", 20, null, null).observe(
            owner,
            Observer { value: MutableList<AlbumID3?>? -> mostPlayedAlbumSample.postValue(value) })
    }

    fun refreshMostRecentlyAddedAlbums(owner: LifecycleOwner) {
        albumRepository.getAlbums("newest", 20, null, null).observe(
            owner,
            Observer { value: MutableList<AlbumID3?>? -> recentlyAddedAlbumSample.postValue(value) })
    }

    fun refreshRecentlyPlayedAlbumList(owner: LifecycleOwner) {
        albumRepository.getAlbums("recent", 20, null, null).observe(
            owner,
            Observer { value: MutableList<AlbumID3?>? -> recentlyPlayedAlbumSample.postValue(value) })
    }

    fun refreshShares(owner: LifecycleOwner) {
        sharingRepository.shares.observe(
            owner,
            Observer { value: MutableList<Share?>? -> this.shares.postValue(value) })
    }

    private fun setHomeSectorList() {
        if (getHomeSectorList() != null && getHomeSectorList() != "null") {
            this.homeSectorList = Gson().fromJson<MutableList<HomeSector?>?>(
                getHomeSectorList(),
                object : TypeToken<MutableList<HomeSector?>?>() {
                }.getType()
            )
        }
    }

    fun checkHomeSectorVisibility(sectorId: String?): Boolean {
        return this.homeSectorList != null && homeSectorList!!
            .any { sector -> sector?.id == sectorId }
    }

    fun setOfflineFavorite() {
        val favorites = this.favorites
        val favoritesToSave = getFavoritesToSave(favorites)
        val favoritesToDelete = getFavoritesToDelete(favorites, favoritesToSave)

        manageFavoriteToSave(favoritesToSave)
        manageFavoriteToDelete(favoritesToDelete)
    }

    private val favorites: ArrayList<Favorite>
        get() = java.util.ArrayList<Favorite>(favoriteRepository.favorites)

    private fun getFavoritesToSave(favorites: java.util.ArrayList<Favorite>): java.util.ArrayList<Favorite> {
        val filteredMap = HashMap<String?, Favorite?>()

        for (favorite in favorites) {
            val key = favorite.toString()

            if (!filteredMap.containsKey(key) || favorite.timestamp > filteredMap.get(key)!!.timestamp) {
                filteredMap.put(key, favorite)
            }
        }

        return java.util.ArrayList<Favorite>(filteredMap.values)
    }

    private fun getFavoritesToDelete(
        favorites: java.util.ArrayList<Favorite>,
        favoritesToSave: java.util.ArrayList<Favorite>
    ): java.util.ArrayList<Favorite?> {
        val favoritesToDelete = java.util.ArrayList<Favorite?>()

        for (favorite in favorites) {
            if (!favoritesToSave.contains(favorite)) {
                favoritesToDelete.add(favorite)
            }
        }

        return favoritesToDelete
    }

    private fun manageFavoriteToSave(favoritesToSave: java.util.ArrayList<Favorite>) {
        for (favorite in favoritesToSave) {
            if (favorite.toStar) {
                favoriteToStar(favorite)
            } else {
                favoriteToUnstar(favorite)
            }
        }
    }

    private fun manageFavoriteToDelete(favoritesToDelete: java.util.ArrayList<Favorite?>) {
        for (favorite in favoritesToDelete) {
            favorite?.let { favoriteRepository.delete(it) }
        }
    }

    private fun favoriteToStar(favorite: Favorite) {
        if (favorite.songId != null) {
            favoriteRepository.star(favorite.songId, null, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        } else if (favorite.albumId != null) {
            favoriteRepository.star(null, favorite.albumId, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        } else if (favorite.artistId != null) {
            favoriteRepository.star(null, null, favorite.artistId, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        }
    }

    private fun favoriteToUnstar(favorite: Favorite) {
        if (favorite.songId != null) {
            favoriteRepository.unstar(favorite.songId, null, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        } else if (favorite.albumId != null) {
            favoriteRepository.unstar(null, favorite.albumId, null, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        } else if (favorite.artistId != null) {
            favoriteRepository.unstar(null, null, favorite.artistId, object : StarCallback {
                override fun onSuccess() {
                    favoriteRepository.delete(favorite)
                }
            })
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
