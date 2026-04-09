package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.subsonic.models.Genre

class GenreCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val genreRepository: GenreRepository

    init {
        genreRepository = GenreRepository()
    }

    val genreList: LiveData<MutableList<Genre?>?>
        get() = genreRepository.getGenres(false, -1)
}
