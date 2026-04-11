package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants.SeedType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.MutableList
import kotlin.collections.MutableSet
import kotlin.collections.mutableListOf
import kotlin.math.min

class SongRepository {
    interface MediaCallbackInternal {
        fun onSongsAvailable(songs: MutableList<Child>?)
    }

    fun getStarredSongs(random: Boolean, size: Int): MutableLiveData<MutableList<Child?>?> {
        val starredSongs = MutableLiveData<MutableList<Child?>?>(mutableListOf<Child?>())

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .starred2
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.starred2 != null) {
                        val songs: List<Child>? =
                            response.body()!!.subsonicResponse.starred2!!.songs

                        if (songs != null) {
                            if (!random) {
                                starredSongs.setValue(ArrayList(songs))
                            } else {
                                val shuffledSongs = ArrayList(songs)
                                Collections.shuffle(shuffledSongs)
                                starredSongs.setValue(ArrayList(shuffledSongs.subList(0, min(size, shuffledSongs.size))))
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
            })

        return starredSongs
    }

    /**
     * Used by ViewModels. Updates the LiveData list incrementally as songs are found.
     */
    fun getInstantMix(
        id: String?,
        type: SeedType,
        count: Int
    ): MutableLiveData<MutableList<Child?>?> {
        val instantMix = MutableLiveData<MutableList<Child?>?>(ArrayList<Child?>())
        val trackIds: MutableSet<String?> = HashSet<String?>()

        performSmartMix(id, type, count, object : MediaCallbackInternal {
            override fun onSongsAvailable(songs: MutableList<Child>?) {
                val current = instantMix.getValue()
                if (current != null) {
                    for (s in songs!!) {
                        if (!trackIds.contains(s.id)) {
                            current.add(s)
                            trackIds.add(s.id)
                        }
                    }

                    if (current.size < count / 2) {
                        fetchSimilarOnly(
                            id,
                            count,
                            object : MediaCallbackInternal {
                                override fun onSongsAvailable(songs: MutableList<Child>?) {
                                    for (r in songs!!) {
                                        if (!trackIds.contains(r.id)) {
                                            current.add(r)
                                            trackIds.add(r.id)
                                        }
                                    }
                                    instantMix.postValue(current)
                                }
                            })
                    } else {
                        instantMix.postValue(current)
                    }
                }
            }
        })

        return instantMix
    }

    /**
     * Overloaded method used by other Repositories
     */
    fun getInstantMix(id: String?, type: SeedType, count: Int, callback: MediaCallbackInternal) {
        MediaCallbackAccumulator(callback, count).start(id, type)
    }

    private inner class MediaCallbackAccumulator(
        private val originalCallback: MediaCallbackInternal,
        private val targetCount: Int
    ) {
        private val accumulatedSongs: MutableList<Child?> = ArrayList<Child?>()
        private val trackIds: MutableSet<String?> = HashSet<String?>()
        private var isComplete = false

        fun start(id: String?, type: SeedType) {
            performSmartMix(
                id,
                type,
                targetCount,
                object : MediaCallbackInternal {
                    override fun onSongsAvailable(songs: MutableList<Child>?) {
                        this@MediaCallbackAccumulator.onBatchReceived(songs)
                    }
                })
        }

        fun onBatchReceived(batch: MutableList<Child>?) {
            if (isComplete || batch == null || batch.isEmpty()) {
                return
            }

            var added = 0
            for (song in batch) {
                if (!trackIds.contains(song.id) && accumulatedSongs.size < targetCount) {
                    trackIds.add(song.id)
                    accumulatedSongs.add(song)
                    added++
                }
            }

            if (accumulatedSongs.size >= targetCount) {
                originalCallback.onSongsAvailable(ArrayList(accumulatedSongs.filterNotNull()))
                isComplete = true
            }
        }
    }

    private fun performSmartMix(
        id: String?,
        type: SeedType,
        count: Int,
        callback: MediaCallbackInternal
    ) {
        when (type) {
            SeedType.ARTIST -> fetchSimilarByArtist(id, count, callback)
            SeedType.ALBUM -> fetchAlbumSongs(id, count, callback)
            SeedType.TRACK -> fetchSingleTrackThenSimilar(id, count, callback)
        }
    }

    private fun fetchAlbumSongs(albumId: String?, count: Int, callback: MediaCallbackInternal) {
        getSubsonicClientInstance(false).browsingClient!!.getAlbum(albumId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.album != null) {
                        val albumSongs: List<Child>? =
                            response.body()!!.subsonicResponse.album!!.songs
                        if (albumSongs != null && !albumSongs.isEmpty()) {
                            val fromAlbum = min(count, albumSongs.size)
                            val limitedAlbumSongs = albumSongs.subList(0, fromAlbum)
                            callback.onSongsAvailable(ArrayList(limitedAlbumSongs))
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.e(TAG, "fetchAlbumSongsThenSimilar.onFailure()", t)
                }
            })
    }

    private fun fetchSimilarByArtist(
        artistId: String?,
        count: Int,
        callback: MediaCallbackInternal
    ) {
        getSubsonicClientInstance(false)
            .browsingClient!!
            .getSimilarSongs2(artistId, count)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    val similar = extractSongs(response, "similarSongs2")
                    Log.d(TAG, "fetchSimilarByArtist.onResponse() - similar songs: " + similar.size)

                    if (!similar.isEmpty()) {
                        val limitedSimilar = ArrayList(similar.subList(0, min(count, similar.size)).filterNotNull())
                        callback.onSongsAvailable(limitedSimilar)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.e(TAG, "fetchSimilarByArtist.onFailure()", t)
                }
            })
    }

    private fun fetchSingleTrackThenSimilar(
        trackId: String?,
        count: Int,
        callback: MediaCallbackInternal
    ) {
        getSubsonicClientInstance(false).browsingClient!!.getSong(trackId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        val song = response.body()!!.subsonicResponse.song
                        if (song != null) {
                            callback.onSongsAvailable(ArrayList(listOfNotNull(song)))
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.e(TAG, "fetchSingleTrackThenSimilar.onFailure()", t)
                }
            })
    }

    private fun fetchSimilarOnly(id: String?, count: Int, callback: MediaCallbackInternal) {
        getSubsonicClientInstance(false).browsingClient!!.getSimilarSongs(id, count)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    val songs = extractSongs(response, "similarSongs")
                    if (!songs.isEmpty()) {
                        val limit = min(count, songs.size)
                        callback.onSongsAvailable(ArrayList(songs.subList(0, limit).filterNotNull()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.e(TAG, "fetchSimilarOnly.onFailure()", t)
                }
            })
    }


    fun getContinuousMix(id: String?, count: Int): MutableLiveData<MutableList<Child?>?> {
        val instantMix = MutableLiveData<MutableList<Child?>?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getSimilarSongs(id, count)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.similarSongs != null) {
                        instantMix.setValue(ArrayList(response.body()!!.subsonicResponse.similarSongs!!.songs!!))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    instantMix.setValue(null)
                }
            })

        return instantMix
    }

    private fun extractSongs(response: Response<ApiResponse?>, type: String): MutableList<Child?> {
        if (response.isSuccessful() && response.body() != null) {
            val res = response.body()!!.subsonicResponse
            var list: List<Child>? = null
            if (type == "similarSongs" && res.similarSongs != null) {
                list = res.similarSongs!!.songs
            } else if (type == "similarSongs2" && res.similarSongs2 != null) {
                list = res.similarSongs2!!.songs
            }
            return if (list != null) ArrayList(list) else ArrayList<Child?>()
        }

        return ArrayList<Child?>()
    }

    fun getRandomSample(
        number: Int,
        fromYear: Int?,
        toYear: Int?
    ): MutableLiveData<MutableList<Child?>?> {
        val randomSongsSample = MutableLiveData<MutableList<Child?>?>()
        getSubsonicClientInstance(false).albumSongListClient!!.getRandomSongs(
            number,
            fromYear,
            toYear
        )?.enqueue(object : Callback<ApiResponse?> {
            override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
                val songs: MutableList<Child?> = ArrayList<Child?>()
                if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.randomSongs != null) {
                    val returned: List<Child>? =
                        response.body()!!.subsonicResponse.randomSongs!!.songs
                    if (returned != null) {
                        songs.addAll(returned)
                    }
                }
                randomSongsSample.setValue(songs)
            }

            override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
        })
        return randomSongsSample
    }

    fun getRandomSampleWithGenre(
        number: Int,
        fromYear: Int?,
        toYear: Int?,
        genre: String?
    ): MutableLiveData<MutableList<Child?>?> {
        val randomSongsSample = MutableLiveData<MutableList<Child?>?>()

        getSubsonicClientInstance(false).albumSongListClient!!.getRandomSongs(
            number,
            fromYear,
            toYear,
            genre
        )?.enqueue(object : Callback<ApiResponse?> {
            override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
                val songs: MutableList<Child?> = ArrayList<Child?>()
                if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.randomSongs != null) {
                    val returned: List<Child>? =
                        response.body()!!.subsonicResponse.randomSongs!!.songs
                    if (returned != null) {
                        songs.addAll(returned)
                    }
                }
                randomSongsSample.setValue(songs)
            }

            override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
        })
        return randomSongsSample
    }

    fun scrobble(id: String?, submission: Boolean) {
        getSubsonicClientInstance(false).mediaAnnotationClient!!.scrobble(id, submission)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
            })
    }

    fun setRating(id: String?, rating: Int) {
        getSubsonicClientInstance(false).mediaAnnotationClient!!.setRating(id, rating)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
            })
    }

    fun getSongsByGenre(id: String?, page: Int): MutableLiveData<MutableList<Child?>?> {
        val songsByGenre = MutableLiveData<MutableList<Child?>?>()
        getSubsonicClientInstance(false).albumSongListClient!!.getSongsByGenre(id, 100, 100 * page)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.songsByGenre != null) {
                        songsByGenre.setValue(ArrayList(response.body()!!.subsonicResponse.songsByGenre!!.songs!!))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
            })
        return songsByGenre
    }

    fun getSongsByGenres(genresId: ArrayList<String?>): MutableLiveData<MutableList<Child?>?> {
        val songsByGenre = MutableLiveData<MutableList<Child?>?>()
        for (id in genresId) {
            getSubsonicClientInstance(false).albumSongListClient!!.getSongsByGenre(id, 500, 0)
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        val songs: MutableList<Child?> = ArrayList<Child?>()
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.songsByGenre != null) {
                            val returned: List<Child>? =
                                response.body()!!.subsonicResponse.songsByGenre!!.songs
                            if (returned != null) {
                                songs.addAll(returned)
                            }
                        }
                        songsByGenre.setValue(songs)
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
                })
        }
        return songsByGenre
    }

    fun getSong(id: String?): MutableLiveData<Child?> {
        val song = MutableLiveData<Child?>()
        getSubsonicClientInstance(false).browsingClient!!.getSong(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        song.setValue(response.body()!!.subsonicResponse.song)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
            })
        return song
    }

    fun getSongLyrics(song: Child): MutableLiveData<String?> {
        val lyrics = MutableLiveData<String?>(null)
        getSubsonicClientInstance(false).mediaRetrievalClient!!.getLyrics(song.artist, song.title)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.lyrics != null) {
                        lyrics.setValue(response.body()!!.subsonicResponse.lyrics!!.value)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {}
            })
        return lyrics
    }

    companion object {
        private const val TAG = "SongRepository"
    }
}