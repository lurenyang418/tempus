package com.cappielloantonio.tempo.repository

import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.Genre
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import java.util.stream.Collectors
import kotlin.math.min

class GenreRepository {
    fun getGenres(random: Boolean, size: Int): MutableLiveData<MutableList<Genre?>?> {
        val genres = MutableLiveData<MutableList<Genre?>?>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .genres
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.genres != null) {
                        val genreList: List<Genre>? =
                            response.body()!!.subsonicResponse.genres!!.genres

                        if (genreList == null || genreList.isEmpty()) {
                            genres.setValue(ArrayList<Genre?>())
                            return
                        }

                        val genreArrayList = ArrayList(genreList)
                        if (random) {
                            Collections.shuffle(genreArrayList)
                        }

                        if (size != -1) {
                            genres.setValue(ArrayList(genreArrayList.subList(0, min(size, genreArrayList.size))))
                        } else {
                            val sorted = genreArrayList.sortedWith(compareBy({ g: Genre? -> g?.genre }))
                            genres.setValue(ArrayList(sorted))
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return genres
    }
}
