package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.ArtistInfo2
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants.SeedType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntBinaryOperator
import java.util.stream.Collectors
import kotlin.math.min

class ArtistRepository {
    private val albumRepository: AlbumRepository

    init {
        this.albumRepository = AlbumRepository()
    }

    fun getArtistAllSongs(artistId: String?, callback: ArtistSongsCallback) {
        Log.d("ArtistSync", "Getting albums for artist: " + artistId)

        // Get the artist info first, which contains the albums
        getSubsonicClientInstance(false)
            .browsingClient!!
            .getArtist(artistId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artist != null && response.body()!!.subsonicResponse.artist!!.albums != null) {
                        val albums: List<AlbumID3>? =
                            response.body()!!.subsonicResponse.artist!!.albums
                        Log.d("ArtistSync", "Got albums directly: " + albums!!.size)

                        if (!albums.isEmpty()) {
                            fetchAllAlbumSongsWithCallback(ArrayList(albums), callback)
                        } else {
                            Log.d("ArtistSync", "No albums found in artist response")
                            callback.onSongsCollected(ArrayList<Child>())
                        }
                    } else {
                        Log.d("ArtistSync", "Failed to get artist info")
                        callback.onSongsCollected(ArrayList<Child>())
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.d("ArtistSync", "Error getting artist info: " + t.message)
                    callback.onSongsCollected(ArrayList<Child>())
                }
            })
    }

    private fun fetchAllAlbumSongsWithCallback(
        albums: MutableList<AlbumID3>?,
        callback: ArtistSongsCallback
    ) {
        if (albums == null || albums.isEmpty()) {
            Log.d("ArtistSync", "No albums to process")
            callback.onSongsCollected(ArrayList<Child>())
            return
        }

        val allSongs: MutableList<Child> = ArrayList<Child>()
        val remainingAlbums = AtomicInteger(albums.size)
        Log.d("ArtistSync", "Processing " + albums.size + " albums")

        for (album in albums) {
            Log.d("ArtistSync", "Getting tracks for album: " + album.name)
            val albumTracks = albumRepository.getAlbumTracks(album.id)
            val observer = object : Observer<MutableList<Child?>?> {
                override fun onChanged(value: MutableList<Child?>?) {
                    Log.d(
                        "ArtistSync",
                        "Got " + (if (value != null) value.size else 0) + " songs from album"
                    )
                    if (value != null) {
                        allSongs.addAll(value.filterNotNull())
                    }
                    albumTracks.removeObserver(this)

                    val remaining = remainingAlbums.decrementAndGet()
                    Log.d("ArtistSync", "Remaining albums: " + remaining)
                    if (remaining == 0) {
                        Log.d("ArtistSync", "All albums processed. Total songs: " + allSongs.size)
                        callback.onSongsCollected(allSongs)
                    }
                }
            }
            albumTracks.observeForever(observer)
        }
    }

    interface ArtistSongsCallback {
        fun onSongsCollected(songs: MutableList<Child>?)
    }

    fun getStarredArtists(random: Boolean, size: Int): MutableLiveData<MutableList<ArtistID3?>?> {
        val starredArtists = MutableLiveData<MutableList<ArtistID3?>?>(ArrayList<ArtistID3?>())

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .starred2
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.starred2 != null) {
                        val artists: List<ArtistID3>? =
                            response.body()!!.subsonicResponse.starred2!!.artists

                        if (artists != null) {
                            if (!random) {
                                getArtistInfo(ArrayList(artists), starredArtists)
                            } else {
                                Collections.shuffle(artists)
                                getArtistInfo(
                                    ArrayList(artists.subList(0, min(size, artists.size))),
                                    starredArtists
                                )
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return starredArtists
    }

    fun getArtists(random: Boolean, size: Int): MutableLiveData<MutableList<ArtistID3?>?> {
        val listLiveArtists = MutableLiveData<MutableList<ArtistID3?>?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .artists
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        val artists: MutableList<ArtistID3> = ArrayList<ArtistID3>()

                        if (response.body()!!.subsonicResponse.artists != null && response.body()!!.subsonicResponse.artists!!.indices != null) {
                            for (index in response.body()!!.subsonicResponse.artists!!.indices!!) {
                                if (index.artists != null) {
                                    artists.addAll(index.artists!!)
                                }
                            }
                        }

                        if (random) {
                            Collections.shuffle(artists)
                            getArtistInfo(
                                artists.subList(
                                    0,
                                    if (artists.size / size > 0) size else artists.size
                                ), listLiveArtists
                            )
                        } else {
                            listLiveArtists.setValue(ArrayList(artists))
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return listLiveArtists
    }

    /*
     * Method that returns essential artist information (cover, album number, etc.)
     */
    fun getArtistInfo(
        artists: MutableList<ArtistID3>,
        list: MutableLiveData<MutableList<ArtistID3?>?>
    ) {
        var liveArtists = list.getValue()
        if (liveArtists == null) liveArtists = ArrayList<ArtistID3?>()
        list.setValue(liveArtists)

        for (artist in artists) {
            getSubsonicClientInstance(false)
                .browsingClient!!
                .getArtist(artist.id)
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artist != null) {
                            addToMutableLiveData(list, response.body()!!.subsonicResponse.artist)
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    }
                })
        }
    }

    fun getArtistInfo(id: String?): MutableLiveData<ArtistID3?> {
        val artist = MutableLiveData<ArtistID3?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getArtist(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artist != null) {
                        artist.setValue(response.body()!!.subsonicResponse.artist)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return artist
    }

    fun getArtistFullInfo(id: String?): MutableLiveData<ArtistInfo2?> {
        val artistFullInfo = MutableLiveData<ArtistInfo2?>(null)

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getArtistInfo2(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artistInfo2 != null) {
                        artistFullInfo.setValue(response.body()!!.subsonicResponse.artistInfo2)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return artistFullInfo
    }

    fun setRating(id: String?, rating: Int) {
        getSubsonicClientInstance(false)
            .mediaAnnotationClient!!
            .setRating(id, rating)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }

    fun getArtist(id: String?): MutableLiveData<ArtistID3?> {
        val artist = MutableLiveData<ArtistID3?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getArtist(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artist != null) {
                        artist.setValue(response.body()!!.subsonicResponse.artist)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return artist
    }

    fun getInstantMix(artist: ArtistID3, count: Int): MutableLiveData<MutableList<Child?>?>? {
        // Delegate to the centralized SongRepository
        return SongRepository().getInstantMix(artist.id, SeedType.ARTIST, count)
    }

    fun getRandomSong(artist: ArtistID3, count: Int): MutableLiveData<MutableList<Child?>?> {
        val randomSongs = MutableLiveData<MutableList<Child?>?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getArtist(artist.id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artist != null && response.body()!!.subsonicResponse.artist!!.albums != null) {
                        val albums: List<AlbumID3>? =
                            response.body()!!.subsonicResponse.artist!!.albums
                        Log.d("ArtistRepository", "Got albums directly: " + albums!!.size)
                        if (albums.isEmpty()) {
                            Log.d("ArtistRepository", "No albums found in artist response")
                            return
                        }

                        val shuffledAlbums = ArrayList(albums)
                        Collections.shuffle(shuffledAlbums)
                        val counts = shuffledAlbums.map { it.songCount ?: 0 }.toIntArray()
                        for (i in 1 until counts.size) counts[i] += counts[i - 1]
                        var albumLimit = 0
                        val multiplier = 4
                        while (albumLimit < shuffledAlbums.size && counts[albumLimit] < count * multiplier) albumLimit++
                        Log.d("ArtistRepository", "Retaining $albumLimit/${shuffledAlbums.size} albums")

                        fetchAllAlbumSongsWithCallback(
                            ArrayList(shuffledAlbums.take(albumLimit)), object : ArtistSongsCallback {
                                override fun onSongsCollected(songs: MutableList<Child>?) {
                                    val songList = songs ?: return
                                    Collections.shuffle(songList)
                                    randomSongs.setValue(ArrayList(songList.take(count)))
                                }
                            })
                    } else {
                        Log.d("ArtistRepository", "Failed to get artist info")
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.d("ArtistRepository", "Error getting artist info: " + t.message)
                }
            })

        return randomSongs
    }

    fun getTopSongs(artistName: String?, count: Int): MutableLiveData<MutableList<Child?>?> {
        val topSongs = MutableLiveData<MutableList<Child?>?>(ArrayList<Child?>())

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getTopSongs(artistName, count)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.topSongs != null && response.body()!!.subsonicResponse.topSongs!!.songs != null) {
                        topSongs.setValue(ArrayList(response.body()!!.subsonicResponse.topSongs!!.songs!!))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return topSongs
    }

    private fun addToMutableLiveData(
        liveData: MutableLiveData<MutableList<ArtistID3?>?>,
        artist: ArtistID3?
    ) {
        val liveArtists = liveData.getValue()
        if (liveArtists != null) liveArtists.add(artist)
        liveData.setValue(liveArtists)
    }
}
