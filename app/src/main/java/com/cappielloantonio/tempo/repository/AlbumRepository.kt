package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.interfaces.DecadesCallback
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.AlbumInfo
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.Constants.SeedType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Collections
import kotlin.math.min

class AlbumRepository {
    fun getAlbums(
        type: String?,
        size: Int,
        fromYear: Int?,
        toYear: Int?
    ): MutableLiveData<MutableList<AlbumID3?>?> {
        val listLiveAlbums = MutableLiveData<MutableList<AlbumID3?>?>(ArrayList<AlbumID3?>())

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .getAlbumList2(type, size, 0, fromYear, toYear)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()
                        && response.body() != null && response.body()!!.subsonicResponse.albumList2 != null && response.body()!!.subsonicResponse.albumList2!!.albums != null
                    ) {
                        listLiveAlbums.setValue(ArrayList(response.body()!!.subsonicResponse.albumList2!!.albums!!))
                    } else {
                        Log.e(
                            "AlbumRepository",
                            "API Error on getAlbums. HTTP Code: " + response.code()
                        )
                        listLiveAlbums.setValue(ArrayList<AlbumID3?>())
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.e("AlbumRepository", "Network Failure on getAlbums: " + t.message)
                    listLiveAlbums.setValue(ArrayList<AlbumID3?>())
                }
            })

        return listLiveAlbums
    }

    fun getStarredAlbums(random: Boolean, size: Int): MutableLiveData<MutableList<AlbumID3?>?> {
        val starredAlbums = MutableLiveData<MutableList<AlbumID3?>?>(ArrayList<AlbumID3?>())

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .starred2
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.starred2 != null) {
                        val albums: List<AlbumID3>? =
                            response.body()!!.subsonicResponse.starred2!!.albums

                        if (albums != null) {
                            if (random) {
                                Collections.shuffle(albums)
                                starredAlbums.setValue(ArrayList(albums.subList(0, min(size, albums.size))))
                            } else {
                                starredAlbums.setValue(ArrayList(albums))
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return starredAlbums
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

    fun getAlbumTracks(id: String?): MutableLiveData<MutableList<Child?>?> {
        val albumTracks = MutableLiveData<MutableList<Child?>?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getAlbum(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    val tracks: MutableList<Child?> = ArrayList<Child?>()

                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.album != null) {
                        if (response.body()!!.subsonicResponse.album!!.songs != null) {
                            tracks.addAll(response.body()!!.subsonicResponse.album!!.songs!!)
                        }
                    }

                    albumTracks.setValue(tracks)
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return albumTracks
    }

    fun getArtistAlbums(id: String?): MutableLiveData<MutableList<AlbumID3?>?> {
        val artistsAlbum = MutableLiveData<MutableList<AlbumID3?>?>(ArrayList<AlbumID3?>())

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getArtist(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artist != null && response.body()!!.subsonicResponse.artist!!.albums != null) {
                        val albums: List<AlbumID3>? =
                            response.body()!!.subsonicResponse.artist!!.albums
                        val sortedAlbums = ArrayList(albums!!)
                        sortedAlbums.sortBy { it.year }
                        Collections.reverse(sortedAlbums)
                        artistsAlbum.setValue(sortedAlbums)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return artistsAlbum
    }

    fun getAlbum(id: String?): MutableLiveData<AlbumID3?> {
        val album = MutableLiveData<AlbumID3?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getAlbum(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.album != null) {
                        album.setValue(response.body()!!.subsonicResponse.album)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return album
    }

    fun getAlbumInfo(id: String?): MutableLiveData<AlbumInfo?> {
        val albumInfo = MutableLiveData<AlbumInfo?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getAlbumInfo2(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.albumInfo != null) {
                        albumInfo.setValue(response.body()!!.subsonicResponse.albumInfo)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return albumInfo
    }

    fun getInstantMix(album: AlbumID3, count: Int): MutableLiveData<MutableList<Child?>?>? {
        // Delegate to the centralized SongRepository
        return SongRepository().getInstantMix(album.id, SeedType.ALBUM, count)
    }


    val decades: MutableLiveData<MutableList<Int?>?>
        get() {
            val decades =
                MutableLiveData<MutableList<Int?>?>()

            getFirstAlbum(object : DecadesCallback {
                override fun onLoadYear(year: Int) {
                    val first = year
                    getLastAlbum(object : DecadesCallback {
                        override fun onLoadYear(year: Int) {
                            if (first != -1 && year != -1) {
                                val decadeList: MutableList<Int?> =
                                    ArrayList<Int?>()

                                var startDecade = first - (first % 10)
                                val lastDecade = year - (year % 10)

                                while (startDecade <= lastDecade) {
                                    decadeList.add(startDecade)
                                    startDecade = startDecade + 10
                                }

                                decades.setValue(decadeList)
                            }
                        }
                    })
                }
            })

            return decades
        }

    private fun getFirstAlbum(callback: DecadesCallback) {
        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .getAlbumList2("byYear", 1, 0, 1900, Calendar.getInstance().get(Calendar.YEAR))
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.albumList2 != null && response.body()!!.subsonicResponse.albumList2!!.albums != null && !response.body()!!.subsonicResponse.albumList2!!.albums!!.isEmpty()) {
                        callback.onLoadYear(
                            response.body()!!.subsonicResponse.albumList2!!.albums!!.get(
                                0
                            ).year
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    callback.onLoadYear(-1)
                }
            })
    }

    private fun getLastAlbum(callback: DecadesCallback) {
        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .getAlbumList2("byYear", 1, 0, Calendar.getInstance().get(Calendar.YEAR), 1900)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.albumList2 != null && response.body()!!.subsonicResponse.albumList2!!.albums != null) {
                        if (!response.body()!!.subsonicResponse.albumList2!!.albums!!.isEmpty() && !response.body()!!.subsonicResponse.albumList2!!.albums!!.isEmpty()) {
                            callback.onLoadYear(
                                response.body()!!.subsonicResponse.albumList2!!.albums!!.get(
                                    0
                                ).year
                            )
                        } else {
                            callback.onLoadYear(-1)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    callback.onLoadYear(-1)
                }
            })
    }
}