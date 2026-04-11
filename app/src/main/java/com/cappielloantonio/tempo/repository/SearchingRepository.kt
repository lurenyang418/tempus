package com.cappielloantonio.tempo.repository

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.RecentSearchDao
import com.cappielloantonio.tempo.model.RecentSearch
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.PlaylistWithSongs
import com.cappielloantonio.tempo.subsonic.models.SearchResult2
import com.cappielloantonio.tempo.subsonic.models.SearchResult3
import com.cappielloantonio.tempo.ui.fragment.SearchFragment
import com.cappielloantonio.tempo.util.Preferences.isSearchSortingChronologicallyEnabled
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.Executors

class SearchingRepository {
    private val recentSearchDao = instance!!.recentSearchDao()

    fun search2(query: String?): MutableLiveData<SearchResult2?> {
        val result = MutableLiveData<SearchResult2?>()

        getSubsonicClientInstance(false)
            .searchingClient!!
            .search3(query, 20, 0, 20, 0, 20, 0)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        result.setValue(response.body()!!.subsonicResponse.searchResult2)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return result
    }

    @UnstableApi
    fun search3(sf: SearchFragment, query: String?): MutableLiveData<SearchResult3?> {
        val result = MutableLiveData<SearchResult3?>()

        Executors.newSingleThreadExecutor().execute(Runnable {
            val allSongs: MutableList<Child?> = ArrayList<Child?>()
            var offset = 0
            val limit = 1000
            var hasMore = true

            while (hasMore) {
                try {
                    val response = getSubsonicClientInstance(false)
                        .searchingClient!!
                        .search3(query, limit, offset, 0, 0, 0, 0)
                        ?.execute()

                    if (response != null && response.isSuccessful && response.body() != null) {
                        val tmp = response.body()!!.subsonicResponse.searchResult3
                        if (tmp != null && tmp.songs != null && !tmp.songs!!.isEmpty()) {
                            val fetchedSongs: List<Child>? = tmp.songs
                            allSongs.addAll(fetchedSongs!!)

                            offset += fetchedSongs.size
                            hasMore = fetchedSongs.size == limit
                        } else {
                            hasMore = false
                        }
                    } else {
                        hasMore = false
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    hasMore = false
                }
            }
            val pws = PlaylistWithSongs("allsongs", allSongs.filterNotNull())
            pws.name = sf.getView()!!.getContext()
                .getString(R.string.search_all_songs, allSongs.size.toString())
            pws.songCount = allSongs.size
            val lpws: MutableList<Playlist?> = ArrayList<Playlist?>()
            lpws.add(pws)
            var duration: Long = 0
            for (song in allSongs) {
                if (song != null && song.duration != null) {
                    duration += song.duration!!.toLong()
                }
            }
            pws.duration = duration
            Handler(Looper.getMainLooper()).post(Runnable {
                sf.updateUI(lpws)
            })
        })

        getSubsonicClientInstance(false)
            .searchingClient!!
            .search3(query, 20, 0, 20, 0, 20, 0)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null) {
                        result.setValue(response.body()!!.subsonicResponse.searchResult3)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return result
    }

    fun getSuggestions(query: String?): MutableLiveData<MutableList<String?>?> {
        val suggestions = MutableLiveData<MutableList<String?>?>()

        getSubsonicClientInstance(false)
            .searchingClient!!
            .search3(query, 5, 0, 5, 0, 5, 0)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    val newSuggestions: MutableList<String?> = ArrayList<String?>()

                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.searchResult3 != null) {
                        if (response.body()!!.subsonicResponse.searchResult3!!.artists != null) {
                            for (artistID3 in response.body()!!.subsonicResponse.searchResult3!!.artists!!) {
                                newSuggestions.add(artistID3.name)
                            }
                        }

                        if (response.body()!!.subsonicResponse.searchResult3!!.albums != null) {
                            for (albumID3 in response.body()!!.subsonicResponse.searchResult3!!.albums!!) {
                                newSuggestions.add(albumID3.name)
                            }
                        }

                        if (response.body()!!.subsonicResponse.searchResult3!!.songs != null) {
                            for (song in response.body()!!.subsonicResponse.searchResult3!!.songs!!) {
                                newSuggestions.add(song.title)
                            }
                        }

                        val hashSet = LinkedHashSet<String?>(newSuggestions)
                        val suggestionsWithoutDuplicates = ArrayList<String?>(hashSet)

                        suggestions.setValue(suggestionsWithoutDuplicates)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return suggestions
    }

    fun insert(recentSearch: RecentSearch) {
        val insert = SearchingRepository.InsertThreadSafe(recentSearchDao!!, recentSearch)
        val thread = Thread(insert)
        thread.start()
    }

    fun delete(recentSearch: RecentSearch) {
        val delete = SearchingRepository.DeleteThreadSafe(recentSearchDao!!, recentSearch)
        val thread = Thread(delete)
        thread.start()
    }

    val recentSearchSuggestion: MutableList<String>
        get() {
            var recent: MutableList<String> =
                ArrayList<String>()

            val suggestionsThread = RecentThreadSafe(recentSearchDao!!)
            val thread = Thread(suggestionsThread)
            thread.start()

            try {
                thread.join()
                recent = suggestionsThread.recent
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return recent
        }

    private class DeleteThreadSafe(
        private val recentSearchDao: RecentSearchDao,
        private val recentSearch: RecentSearch
    ) : Runnable {
        override fun run() {
            recentSearchDao.delete(recentSearch)
        }
    }

    private class InsertThreadSafe(
        private val recentSearchDao: RecentSearchDao,
        private val recentSearch: RecentSearch
    ) : Runnable {
        override fun run() {
            recentSearchDao.insert(recentSearch)
        }
    }

    private class RecentThreadSafe(private val recentSearchDao: RecentSearchDao) : Runnable {
        var recent: MutableList<String> = ArrayList<String>()
            private set

        override fun run() {
            if (isSearchSortingChronologicallyEnabled()) {
                recent = recentSearchDao.recent
            } else {
                recent = recentSearchDao.alpha
            }
        }
    }
}
