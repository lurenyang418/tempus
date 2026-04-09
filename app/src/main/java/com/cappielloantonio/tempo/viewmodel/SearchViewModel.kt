package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.model.RecentSearch
import com.cappielloantonio.tempo.repository.SearchingRepository
import com.cappielloantonio.tempo.subsonic.models.SearchResult2
import com.cappielloantonio.tempo.subsonic.models.SearchResult3
import com.cappielloantonio.tempo.ui.fragment.SearchFragment

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var query: String? = ""
        set(query) {
            field = query

            if (!query!!.isEmpty()) {
                insertNewSearch(query)
            }
        }

    private val searchingRepository: SearchingRepository

    init {
        searchingRepository = SearchingRepository()
    }

    fun search2(title: String?): LiveData<SearchResult2?> {
        return searchingRepository.search2(title)
    }

    @UnstableApi
    fun search3(sf: SearchFragment, title: String?): LiveData<SearchResult3?> {
        return searchingRepository.search3(sf, title)
    }

    fun insertNewSearch(search: String) {
        searchingRepository.insert(RecentSearch(search, System.currentTimeMillis() / 1000L))
    }

    fun deleteRecentSearch(search: String) {
        searchingRepository.delete(RecentSearch(search, 0))
    }

    fun getSearchSuggestion(query: String?): LiveData<MutableList<String?>?> {
        return searchingRepository.getSuggestions(query)
    }

    val recentSearchSuggestion: MutableList<String?>
        get() {
            val suggestions =
                ArrayList<String?>()
            suggestions.addAll(searchingRepository.recentSearchSuggestion!!)

            return suggestions
        }

    companion object {
        private const val TAG = "SearchViewModel"
    }
}
