package com.cappielloantonio.tempo.repository

import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.FavoriteDao
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Favorite
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavoriteRepository {
    private val favoriteDao = instance!!.favoriteDao()

    fun star(id: String?, albumId: String?, artistId: String?, starCallback: StarCallback) {
        getSubsonicClientInstance(false)
            .mediaAnnotationClient!!
            .star(id, albumId, artistId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()) {
                        starCallback.onSuccess()
                    } else {
                        starCallback.onError()
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    starCallback.onError()
                }
            })
    }

    fun unstar(id: String?, albumId: String?, artistId: String?, starCallback: StarCallback) {
        getSubsonicClientInstance(false)
            .mediaAnnotationClient!!
            .unstar(id, albumId, artistId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()) {
                        starCallback.onSuccess()
                    } else {
                        starCallback.onError()
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    starCallback.onError()
                }
            })
    }

    val favorites: MutableList<Favorite>
        get() {
            var favorites: MutableList<Favorite> =
                ArrayList<Favorite>()

            val getAllThreadSafe = GetAllThreadSafe(favoriteDao!!)
            val thread = Thread(getAllThreadSafe)
            thread.start()

            try {
                thread.join()
                favorites = getAllThreadSafe.favorites
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return favorites
        }

    private class GetAllThreadSafe(private val favoriteDao: FavoriteDao) : Runnable {
            var favorites: MutableList<Favorite> = ArrayList<Favorite>()
            private set

        override fun run() {
            favorites = favoriteDao.all
        }
    }

    fun starLater(id: String?, albumId: String?, artistId: String?, toStar: Boolean) {
        val insert = FavoriteRepository.InsertThreadSafe(
            favoriteDao!!,
            Favorite(System.currentTimeMillis(), id, albumId, artistId, toStar)
        )
        val thread = Thread(insert)
        thread.start()
    }

    private class InsertThreadSafe(
        private val favoriteDao: FavoriteDao,
        private val favorite: Favorite
    ) : Runnable {
        override fun run() {
            favoriteDao.insert(favorite)
        }
    }

    fun delete(favorite: Favorite) {
        val delete = FavoriteRepository.DeleteThreadSafe(favoriteDao!!, favorite)
        val thread = Thread(delete)
        thread.start()
    }

    private class DeleteThreadSafe(
        private val favoriteDao: FavoriteDao,
        private val favorite: Favorite
    ) : Runnable {
        override fun run() {
            favoriteDao.delete(favorite)
        }
    }
}
