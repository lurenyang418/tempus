package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.subsonic.models.Genre

class FilterViewModel(application: Application) : AndroidViewModel(application) {
    private val genreRepository: GenreRepository

    val filters: ArrayList<String?> = ArrayList<String?>()
    val filterNames: ArrayList<String?> = ArrayList<String?>()

    init {
        genreRepository = GenreRepository()
    }

    val genreList: LiveData<MutableList<Genre?>?>
        get() = genreRepository.getGenres(false, -1)

    fun addFilter(filterID: String?, filterName: String?) {
        filters.add(filterID)
        filterNames.add(filterName)
    }

    fun removeFilter(filterID: String?, filterName: String?) {
        filters.remove(filterID)
        filterNames.remove(filterName)
    }
}
